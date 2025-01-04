package com.devoid.menumate.prsentation.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.room.Room
import androidx.transition.AutoTransition
import androidx.transition.ChangeBounds
import androidx.transition.ChangeImageTransform
import androidx.transition.ChangeTransform
import androidx.transition.Explode
import androidx.transition.Fade
import androidx.transition.Transition
import androidx.transition.TransitionSet
import com.devoid.menumate.R
import com.devoid.menumate.data.remote.TableManager
import com.devoid.menumate.data.remote.TableState
import com.devoid.menumate.data.roomsql.AppDatabase
import com.devoid.menumate.databinding.HomePageBinding
import com.devoid.menumate.domain.model.MenuItem
import com.devoid.menumate.domain.model.TableInfo
import com.devoid.menumate.prsentation.ui.fragments.FoodItemFragment
import com.devoid.menumate.prsentation.ui.fragments.HomepageFragment
import com.devoid.menumate.prsentation.ui.fragments.QrCodeReaderFragment
import com.google.android.material.transition.MaterialContainerTransform
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject
    lateinit var tableManager: dagger.Lazy<TableManager>
    lateinit var appDatabase: AppDatabase
    private var homepageFragment: HomepageFragment? = null
    private final val TAG = this::class.simpleName
    private lateinit var binding: HomePageBinding
    private var currentUser: FirebaseUser? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = HomePageBinding.inflate(layoutInflater)
        setContentView(binding.root)
        currentUser = Firebase.auth.currentUser
        onBackPressedDispatcher.addCallback {
            finishAffinity()
        }
        if (currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        } else {
            init()
            observeTableManager()
            getTAbleInfoFromIntent()
        }
    }

    private fun observeTableManager() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED){
                tableManager.get().tableState.collect { tableState ->
                    when (tableState) {
                        is TableState.Initialized -> {
                            showHomeFragment()
                        }
                        TableState.UnInitialized -> {}
                    }
                }
            }
        }
    }

    private fun getTAbleInfoFromIntent() {
        val appLinkIntent: Intent = intent
        val appLinkAction: String? = appLinkIntent.action
        val appLinkData: Uri? = appLinkIntent.data
        if (appLinkAction == Intent.ACTION_VIEW && appLinkData != null) {
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            tableManager.get().initialize(
                TableInfo(
                    appLinkData.getQueryParameter("id")!!,
                    appLinkData.getQueryParameter("table")!!.toInt()
                )
            )
            showHomeFragment()
        } else {
            showFragment(QrCodeReaderFragment())
        }
    }

    private fun init() {
        appDatabase =
            Room.databaseBuilder(applicationContext, AppDatabase::class.java, "local-database")
                .build()
    }

    private fun showHomeFragment() {
        homepageFragment = HomepageFragment()
        supportFragmentManager.beginTransaction().apply {
            replace(this@MainActivity.binding.fragmentContainer.id, homepageFragment!!)
                .addToBackStack(null)
        }.commit()
    }

    fun showFragment(fragment: Fragment) {
        val transaction =
            supportFragmentManager.beginTransaction().add(binding.fragmentContainer.id, fragment)
                .setCustomAnimations(R.anim.slide_in, R.anim.slide_out)
                .addToBackStack(null)
        homepageFragment?.let {
            transaction.hide(it)
        }
        homepageFragment?.let {
            onBackPressedDispatcher.addCallback(this){
                supportFragmentManager.popBackStack()
                supportFragmentManager.beginTransaction().remove(fragment)
                    .commit()
                remove()
            }
        }
        transaction.commit()

    }

    fun showFragmentTransition(itemInstance: MenuItem,root:ViewGroup, image: View, title: View, price: View) {
        val fragment = FoodItemFragment(
            itemInstance,
            root.transitionName,
            image.transitionName,
            title.transitionName,
            price.transitionName
        ).apply {
            sharedElementEnterTransition = getTransition()
            sharedElementReturnTransition = getTransition()
        }
        val transaction = supportFragmentManager.beginTransaction()
            .apply {
                addSharedElement(image, image.transitionName)
                addSharedElement(root, root.transitionName)
                addSharedElement(title, title.transitionName)
                addSharedElement(price, price.transitionName)
                add(this@MainActivity.binding.fragmentContainer.id, fragment)
                addToBackStack(null)
            }
        homepageFragment?.let {
            transaction.hide(it)
            onBackPressedDispatcher.addCallback(this) {
                supportFragmentManager.popBackStack()
                supportFragmentManager.beginTransaction().remove(fragment)
                    .commit()
                remove()
            }

        }
        transaction.commit()
    }

    private fun getTransition(): Transition {
        val set: TransitionSet = TransitionSet().apply {
            setOrdering(TransitionSet.ORDERING_TOGETHER)
            addTransition(AutoTransition())
        }
        return set
    }

}