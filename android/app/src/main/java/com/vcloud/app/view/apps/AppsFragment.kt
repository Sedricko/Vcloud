package com.vcloud.app.view.apps

import android.graphics.Point
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import cbfg.rvadapter.RVAdapter
import com.afollestad.materialdialogs.MaterialDialog
import com.vcloud.motor.VcloudCore
import com.vcloud.app.R
import com.vcloud.app.bean.AppInfo
import com.vcloud.app.databinding.FragmentAppsBinding
import com.vcloud.app.util.InjectionUtil
import com.vcloud.app.util.ShortcutUtil
import com.vcloud.app.util.inflate
import com.vcloud.app.util.MemoryManager
import com.vcloud.app.util.toast
import com.vcloud.app.view.base.LoadingActivity
import com.vcloud.app.view.main.MainActivity
import java.util.*
import kotlin.math.abs



class AppsFragment : Fragment() {

    var userID: Int = 0

    private lateinit var viewModel: AppsViewModel

    private lateinit var mAdapter: RVAdapter<AppInfo>

    private val viewBinding: FragmentAppsBinding by inflate()

    private var popupMenu: PopupMenu? = null
    private var ajudanteDeArrastar: ItemTouchHelper? = null

    companion object {
        private const val TAG = "AppsFragment"
        
        fun newInstance(userID:Int): AppsFragment {
            val fragment = AppsFragment()
            val bundle = bundleOf("userID" to userID)
            fragment.arguments = bundle
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            super.onCreate(savedInstanceState)
            viewModel =
                ViewModelProvider(this, InjectionUtil.getAppsFactory()).get(AppsViewModel::class.java)
            userID = requireArguments().getInt("userID", 0)
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate: ${e.message}")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        try {
            viewBinding.stateView.showEmpty()

            mAdapter =
                RVAdapter<AppInfo>(requireContext(), AppsAdapter()).bind(viewBinding.recyclerView)

            viewBinding.recyclerView.adapter = mAdapter
            
            
            // Colunas pela largura da tela (a caixa de apps é ~90dp mais estreita que a janela): 4 no S25 em pé, mais deitado.
            val larguraDp = resources.displayMetrics.widthPixels / resources.displayMetrics.density
            val layoutManager = GridLayoutManager(requireContext(), ((larguraDp - 90) / 80).toInt().coerceAtLeast(3))
            layoutManager.isItemPrefetchEnabled = true
            layoutManager.initialPrefetchItemCount = 8
            viewBinding.recyclerView.layoutManager = layoutManager
            
            
            viewBinding.recyclerView.setItemViewCacheSize(20)
            viewBinding.recyclerView.setHasFixedSize(true)
            
            
            viewBinding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    try {
                        super.onScrollStateChanged(recyclerView, newState)
                        when (newState) {
                            RecyclerView.SCROLL_STATE_IDLE -> {
                                
                                MemoryManager.optimizeMemoryForRecyclerView()
                            }
                            RecyclerView.SCROLL_STATE_DRAGGING -> {
                                
                                
                            }
                            RecyclerView.SCROLL_STATE_SETTLING -> {
                                
                                
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in scroll state change: ${e.message}")
                    }
                }
                
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    try {
                        super.onScrolled(recyclerView, dx, dy)
                        
                        if (Math.abs(dy) > 100) {
                            
                            
                            
                            if (MemoryManager.isMemoryCritical()) {
                                Log.w(TAG, "Memory critical during fast scrolling, forcing GC")
                                MemoryManager.forceGarbageCollectionIfNeeded()
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in scroll: ${e.message}")
                    }
                }
            })

            val touchCallBack = AppsTouchCallBack { from, to ->
                try {
                    onItemMove(from, to)
                    viewModel.updateSortLiveData.postValue(true)
                } catch (e: Exception) {
                    Log.e(TAG, "Error in touch callback: ${e.message}")
                }
            }

            val itemTouchHelper = ItemTouchHelper(touchCallBack)
            ajudanteDeArrastar = itemTouchHelper
            itemTouchHelper.attachToRecyclerView(viewBinding.recyclerView)

            mAdapter.setItemClickListener { _, data, _ ->
                try {
                    com.vcloud.motor.utils.Diagnostico.evento("TOQUE curto abriu o app ${data.packageName}")
                    showLoading()
                    viewModel.launchApk(data.packageName, userID)
                } catch (e: Exception) {
                    Log.e(TAG, "Error launching app: ${e.message}")
                    hideLoading()
                }
            }

            interceptTouch()
            setOnLongClick()
            return viewBinding.root
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreateView: ${e.message}")
            
            return View(requireContext())
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        try {
            super.onViewCreated(view, savedInstanceState)
            initData()
        } catch (e: Exception) {
            Log.e(TAG, "Error in onViewCreated: ${e.message}")
        }
    }

    override fun onStart() {
        try {
            super.onStart()
            
            
            try {
                VcloudCore.get().addServiceAvailableCallback {
                    Log.d(TAG, "Services became available, refreshing app list")
                    
                    viewModel.getInstalledAppsWithRetry(userID)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error registering service available callback: ${e.message}")
            }
            
            viewModel.getInstalledAppsWithRetry(userID)
        } catch (e: Exception) {
            Log.e(TAG, "Error in onStart: ${e.message}")
        }
    }

    
    private fun interceptTouch() {
        try {
            val point = Point()
            var isScrolling = false
            var scrollStartTime = 0L
            
            viewBinding.recyclerView.setOnTouchListener { _, e ->
                try {
                    when (e.action) {
                        MotionEvent.ACTION_DOWN -> {
                            com.vcloud.motor.utils.Diagnostico.eventoLimitado("menu|down", 800, "TOQUE na lista de apps: dedo desceu em x=${e.rawX.toInt()} y=${e.rawY.toInt()}")
                            
                            isScrolling = false
                            scrollStartTime = System.currentTimeMillis()
                            point.set(0, 0)
                        }
                        
                        MotionEvent.ACTION_UP -> {
                            val scrollDuration = System.currentTimeMillis() - scrollStartTime
                            val moveu = isMove(point, e)
                            if (popupMenu != null) {
                                // diagnóstico: o que decidiu mostrar ou descartar o menu ao soltar o dedo
                                com.vcloud.motor.utils.Diagnostico.evento("MENU soltou o dedo: durou=${scrollDuration}ms rolando=$isScrolling moveu=$moveu -> " +
                                    if (!isScrolling && !moveu && scrollDuration < 15000) "mostra o menu" else "DESCARTA o menu")
                            }
                            
                            if (!isScrolling && !moveu && scrollDuration < 15000) {
                                try {
                                    popupMenu?.show()
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error showing popup menu: ${e.message}")
                                }
                            }
                            
                            popupMenu = null
                            point.set(0, 0)
                            isScrolling = false
                        }

                        MotionEvent.ACTION_MOVE -> {
                            if (point.x == 0 && point.y == 0) {
                                point.x = e.rawX.toInt()
                                point.y = e.rawY.toInt()
                            }
                            
                            
                            if (isMove(point, e)) {
                                isScrolling = true
                                if (popupMenu != null) {
                                    com.vcloud.motor.utils.Diagnostico.eventoLimitado("menu|moveu", 3000, "MENU dedo se moveu depois do toque longo: começa o arrastar")
                                    // toque longo + mover = reordenar os apps (o arrastar não começa mais sozinho no toque longo)
                                    try {
                                        val filho = viewBinding.recyclerView.findChildViewUnder(e.x, e.y)
                                        if (filho != null) ajudanteDeArrastar?.startDrag(viewBinding.recyclerView.getChildViewHolder(filho))
                                    } catch (ex: Exception) {
                                        Log.e(TAG, "Error starting drag: ${ex.message}")
                                    }
                                }
                                popupMenu?.dismiss()
                            }
                            
                            
                            isDownAndUp(point, e)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error in touch listener: ${e.message}")
                }
                return@setOnTouchListener false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in interceptTouch: ${e.message}")
        }
    }

    private fun isMove(point: Point, e: MotionEvent): Boolean {
        return try {
            val max = 40

            val x = point.x
            val y = point.y

            val xU = abs(x - e.rawX)
            val yU = abs(y - e.rawY)
            xU > max || yU > max
        } catch (e: Exception) {
            Log.e(TAG, "Error in isMove: ${e.message}")
            false
        }
    }

    private fun isDownAndUp(point: Point, e: MotionEvent) {
        try {
            val min = 10
            val y = point.y
            val yU = y - e.rawY

            if (abs(yU) > min) {
                try {
                    (requireActivity() as? MainActivity)?.showFloatButton(yU < 0)
                } catch (e: Exception) {
                    Log.e(TAG, "Error showing/hiding float button: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in isDownAndUp: ${e.message}")
        }
    }

    private fun onItemMove(fromPosition: Int, toPosition: Int) {
        try {
            
            val items = mAdapter.getItems()
            if (fromPosition < 0 || toPosition < 0 || 
                fromPosition >= items.size || toPosition >= items.size) {
                Log.w(TAG, "Invalid positions for move: from=$fromPosition, to=$toPosition, size=${items.size}")
                return
            }
            
            if (fromPosition < toPosition) {
                for (i in fromPosition until toPosition) {
                    try {
                        Collections.swap(items, i, i + 1)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error swapping items at position $i: ${e.message}")
                        return
                    }
                }
            } else {
                for (i in fromPosition downTo toPosition + 1) {
                    try {
                        Collections.swap(items, i, i - 1)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error swapping items at position $i: ${e.message}")
                        return
                    }
                }
            }
            
            try {
                mAdapter.notifyItemMoved(fromPosition, toPosition)
            } catch (e: Exception) {
                Log.e(TAG, "Error notifying item moved: ${e.message}")
                
                mAdapter.notifyDataSetChanged()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in onItemMove: ${e.message}")
        }
    }

    private val pedirPermissoes =
            registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()) { r ->
                val negadas = r.count { !it.value }
                toast(if (negadas == 0) "Permissões liberadas" else "$negadas permissão(ões) não liberada(s)")
            }

    /**
     * Vcloud: os apps de dentro usam as permissões do próprio Vcloud. Pega as permissões "perigosas" que o app pede
     * (câmera, microfone, localização...), que o Vcloud também declara e ainda não tem, e pede todas de uma vez ao Android.
     */
    private fun darPermissoes(pacote: String) {
        try {
            val ctx = requireContext()
            val pm = ctx.packageManager
            val doApp = com.vcloud.motor.fake.frameworks.BPackageManager.get()
                    .getPackageInfo(pacote, android.content.pm.PackageManager.GET_PERMISSIONS, userID)
                    ?.requestedPermissions?.toSet() ?: emptySet()
            val doVcloud = pm.getPackageInfo(ctx.packageName, android.content.pm.PackageManager.GET_PERMISSIONS)
                    .requestedPermissions?.toSet() ?: emptySet()
            val faltando = doApp.filter { p ->
                p in doVcloud &&
                        p != android.Manifest.permission.ACCESS_BACKGROUND_LOCATION && // o Android exige pedir separado
                        androidx.core.content.ContextCompat.checkSelfPermission(ctx, p) != android.content.pm.PackageManager.PERMISSION_GRANTED &&
                        try {
                            (pm.getPermissionInfo(p, 0).protectionLevel and android.content.pm.PermissionInfo.PROTECTION_MASK_BASE) ==
                                    android.content.pm.PermissionInfo.PROTECTION_DANGEROUS
                        } catch (e: Exception) { false }
            }
            val precisaArquivos = doApp.any { it.endsWith("EXTERNAL_STORAGE") } &&
                    android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R &&
                    !android.os.Environment.isExternalStorageManager()
            if (precisaArquivos) {
                startActivity(android.content.Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        android.net.Uri.parse("package:" + ctx.packageName)))
            }
            // Vcloud: "Sobrepor a outros apps" (rodar sobre outros) é especial: o Android só libera pela tela própria.
            val precisaSobrepor = android.Manifest.permission.SYSTEM_ALERT_WINDOW in doApp &&
                    android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M &&
                    !android.provider.Settings.canDrawOverlays(ctx)
            if (precisaSobrepor) {
                startActivity(android.content.Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        android.net.Uri.parse("package:" + ctx.packageName)))
            }
            if (faltando.isEmpty()) {
                if (!precisaArquivos && !precisaSobrepor) toast("Este app já tem as permissões que precisa")
                return
            }
            pedirPermissoes.launch(faltando.toTypedArray())
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao pedir permissões: ${e.message}")
            toast("Não consegui ler as permissões deste app")
        }
    }

    private fun setOnLongClick() {
        try {
            mAdapter.setItemLongClickListener { view, data, _ ->
                try {
                    com.vcloud.motor.utils.Diagnostico.evento("MENU toque longo no app ${data.packageName}")
                    popupMenu = PopupMenu(requireContext(),view).also {
                        it.inflate(R.menu.app_menu)
                        it.setOnMenuItemClickListener { item ->
                            try {
                                com.vcloud.motor.utils.Diagnostico.evento("MENU escolheu o item '${item.title}' para ${data.packageName}")
                                when (item.itemId) {
                                    R.id.app_remove -> {
                                        com.vcloud.motor.utils.Diagnostico.evento("MENU escolheu Desinstalar app: ${data.packageName}")
                                        if (data.isXpModule) {
                                            toast(R.string.uninstall_module_toast)
                                        } else {
                                            unInstallApk(data)
                                        }
                                    }

                                    R.id.app_clear -> {
                                        clearApk(data)
                                    }

                                    R.id.app_stop -> {
                                        stopApk(data)
                                    }

                                    R.id.app_shortcut -> {
                                        ShortcutUtil.createShortcut(requireContext(), userID, data)
                                    }

                                    R.id.app_permissions -> {
                                        darPermissoes(data.packageName)
                                    }

                                    R.id.app_root -> {
                                        startActivity(android.content.Intent(requireContext(), com.vcloud.app.view.root.RootManagerActivity::class.java))
                                    }
                                }
                                return@setOnMenuItemClickListener true
                            } catch (e: Exception) {
                                Log.e(TAG, "Error in menu item click: ${e.message}")
                                return@setOnMenuItemClickListener false
                            }
                        }
                        it.show()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error in long click: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in setOnLongClick: ${e.message}")
        }
    }
    
    private fun initData() {
        try {
            viewBinding.stateView.showLoading()
            viewModel.getInstalledApps(userID)
            viewModel.appsLiveData.observe(viewLifecycleOwner) {
                try {
                    if (it != null) {
                        mAdapter.setItems(it)
                        if (it.isEmpty()) {
                            viewBinding.stateView.showEmpty()
                        } else {
                            viewBinding.stateView.showContent()
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error observing apps data: ${e.message}")
                }
            }

            viewModel.resultLiveData.observe(viewLifecycleOwner) {
                try {
                    if (!TextUtils.isEmpty(it)) {
                        hideLoading()
                        requireContext().toast(it)
                        viewModel.getInstalledApps(userID)
                        scanUser()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error observing result data: ${e.message}")
                }
            }

            viewModel.launchLiveData.observe(viewLifecycleOwner) {
                try {
                    it?.run {
                        hideLoading()
                        if (!it) {
                            toast(R.string.start_fail)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error observing launch data: ${e.message}")
                }
            }

            viewModel.updateSortLiveData.observe(viewLifecycleOwner) {
                try {
                    if (this::mAdapter.isInitialized) {
                        viewModel.updateApkOrder(userID, mAdapter.getItems())
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error observing sort data: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in initData: ${e.message}")
        }
    }

    override fun onStop() {
        try {
            super.onStop()
            viewModel.resultLiveData.value = null
            viewModel.launchLiveData.value = null
        } catch (e: Exception) {
            Log.e(TAG, "Error in onStop: ${e.message}")
        }
    }

    private fun unInstallApk(info: AppInfo) {
        try {
            MaterialDialog(requireContext()).show {
                title(R.string.uninstall_app)
                message(text = getString(R.string.uninstall_app_hint, info.name))
                positiveButton(R.string.done) {
                    try {
                        com.vcloud.motor.utils.Diagnostico.evento("DESINSTALAR confirmado na tela do Vcloud: ${info.packageName}")
                        showLoading()
                        viewModel.unInstall(info.packageName, userID)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error uninstalling app: ${e.message}")
                        hideLoading()
                    }
                }
                negativeButton(R.string.cancel)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error showing uninstall dialog: ${e.message}")
        }
    }

    
    private fun stopApk(info: AppInfo) {
        try {
            MaterialDialog(requireContext()).show {
                title(R.string.app_stop)
                message(text = getString(R.string.app_stop_hint,info.name))
                positiveButton(R.string.done) {
                    try {
                        VcloudCore.get().stopPackage(info.packageName, userID)
                        toast(getString(R.string.is_stop,info.name))
                    } catch (e: Exception) {
                        Log.e(TAG, "Error stopping app: ${e.message}")
                    }
                }
                negativeButton(R.string.cancel)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error showing stop dialog: ${e.message}")
        }
    }

    
    private fun clearApk(info: AppInfo) {
        try {
            MaterialDialog(requireContext()).show {
                title(R.string.app_clear)
                message(text = getString(R.string.app_clear_hint,info.name))
                positiveButton(R.string.done) {
                    try {
                        showLoading()
                        viewModel.clearApkData(info.packageName, userID)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error clearing app data: ${e.message}")
                        hideLoading()
                    }
                }
                negativeButton(R.string.cancel)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error showing clear dialog: ${e.message}")
        }
    }

    fun installApk(source: String) {
        try {
            showLoading()
            viewModel.install(source, userID)
        } catch (e: Exception) {
            Log.e(TAG, "Error installing APK: ${e.message}")
            hideLoading()
        }
    }

    private fun scanUser() {
        try {
            (requireActivity() as? MainActivity)?.scanUser()
        } catch (e: Exception) {
            Log.e(TAG, "Error scanning user: ${e.message}")
        }
    }

    private fun showLoading() {
        try {
            if(requireActivity() is LoadingActivity){
                (requireActivity() as LoadingActivity).showLoading()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error showing loading: ${e.message}")
        }
    }

    private fun hideLoading() {
        try {
            if(requireActivity() is LoadingActivity){
                (requireActivity() as LoadingActivity).hideLoading()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error hiding loading: ${e.message}")
        }
    }
}
