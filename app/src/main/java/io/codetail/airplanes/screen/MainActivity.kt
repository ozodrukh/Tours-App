package io.codetail.airplanes.screen

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.graphics.Rect
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentStatePagerAdapter
import android.support.v4.app.NotificationManagerCompat
import android.support.v4.view.ViewPager
import android.support.v7.app.NotificationCompat
import android.text.format.Time
import android.view.View
import android.widget.ImageView
import com.google.firebase.database.FirebaseDatabase
import io.codetail.airplanes.R
import io.codetail.airplanes.ToursApp
import io.codetail.airplanes.databinding.ActivityMainBinding
import io.codetail.airplanes.domain.Random
import io.codetail.airplanes.domain.TICKET_DATE_FORMAT
import io.codetail.airplanes.domain.UserInterestType
import io.codetail.airplanes.ext.dp
import io.codetail.airplanes.ext.get
import io.codetail.airplanes.global.LazyDataBindingComponent
import io.codetail.airplanes.global.LifecycleActivity
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import timber.log.Timber


/**
 * created at 5/19/17
 * @author Ozodrukh
 * *
 * @version 1.0
 */
class MainActivity : LifecycleActivity() {
    val componentLoader = LazyDataBindingComponent<ActivityMainBinding>();
    val viewComponent by componentLoader;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        componentLoader.inflate(layoutInflater, R.layout.activity_main)

        BackgroundLoader.dispatch(this, Rect(0, 0, 300.dp(this), 300.dp(this))) { drawable ->
            viewComponent.parallaxContainer.setScaleType(ImageView.ScaleType.CENTER_CROP)
            viewComponent.parallaxContainer.setImageDrawable(drawable)
            viewComponent.parallaxContainer.setOnClickListener(object : View.OnClickListener {
                var clickedTimes = 5

                override fun onClick(view: View) {
                    clickedTimes--

                    if (clickedTimes <= 3 && clickedTimes != 1) {
                        Snackbar.make(viewComponent.container, "$clickedTimes till DevOps", Snackbar.LENGTH_SHORT).show()
                    }

                    if (clickedTimes == 0 && viewComponent.toolbar.menu.size() == 0) {
                        viewComponent.toolbar.inflateMenu(R.menu.dev_ops_menu)
                        viewComponent.parallaxContainer.setOnClickListener(null)

                        Snackbar.make(viewComponent.container, "DevOps menu created", Snackbar.LENGTH_SHORT).show()
                    }
                }

            })
        }

        setContentView(viewComponent.root)

        ViewModelProviders.of(this)
                .get<TicketsViewModel>()
                .apply { Timber.d("Instance hash: $this") }

        viewComponent.toolbar.setOnMenuItemClickListener { menu ->
            val viewModel = ViewModelProviders.of(this).get<TicketsViewModel>()
            when (menu.itemId) {
                R.id.action_add_random_ticket ->
                    viewModel.add(Random.randomTicket(-1))

                R.id.action_export_tickets ->
                    viewModel.tickets().subscribe { tickets ->
                        FirebaseDatabase.getInstance().getReference("data").setValue(tickets) { error, ref ->
                            val message: String = if (error == null) "Synced" else error.message
                            Snackbar.make(viewComponent.container, message, Snackbar.LENGTH_SHORT).show()
                        }
                    }
            }
            return@setOnMenuItemClickListener true
        }

        viewComponent.container.setAdapter(object : FragmentStatePagerAdapter(supportFragmentManager) {
            override fun getItem(position: Int): Fragment {
                val fragment = TicketsViewFragment()
                fragment.arguments = Bundle().apply {
                    putString("dataType", when (position) {
                        0 -> TicketsViewFragment.DataType.LIVE
                        1 -> TicketsViewFragment.DataType.INTERESTED
                        2 -> TicketsViewFragment.DataType.GOING
                        else -> TicketsViewFragment.DataType.NO_TICKETS
                    }.name)
                }
                return fragment
            }

            override fun getPageTitle(position: Int): CharSequence {
                return when (position) {
                    0 -> "Live Tickets"
                    1 -> "Interested"
                    2 -> "Going"
                    else -> "No getTickets"
                }
            }

            override fun getCount(): Int {
                return 3;
            }
        })

        viewComponent.container.addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
            override fun onPageSelected(position: Int) {
                viewComponent.container.adapter as FragmentStatePagerAdapter
                val title = viewComponent.container.adapter.getPageTitle(position)
                viewComponent.toolbar.title = title
                viewComponent.collapsingToolbar.title = title
                viewComponent.navigation.setSelectedItemId(when (position) {
                    0 -> R.id.action_page_home
                    1 -> R.id.action_page_interested
                    2 -> R.id.action_page_going
                    else -> R.id.action_page_home
                })
            }
        })

        viewComponent.navigation.setOnNavigationItemSelectedListener { menu ->
            viewComponent.container.setCurrentItem(when (menu.itemId) {
                R.id.action_page_home -> 0
                R.id.action_page_interested -> 1
                R.id.action_page_going -> 2
                else -> 0
            }, true)
            return@setOnNavigationItemSelectedListener true
        }

        observeGoings()
    }

    fun observeGoings() {
        ToursApp.self.database.ticketsService().find(UserInterestType.GOING)
                .doOnNext { Timber.d("Raw Goings = $it") }
                .flatMap { Flowable.fromArray(*it.toTypedArray()) }
                .filter { isTomorrow(TICKET_DATE_FORMAT.parse(it.departure.date).time) }
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext {
                    Timber.d("Going to $it")
                    NotificationManagerCompat.from(this).notify(it.id, NotificationCompat.Builder(this)
                            .setSmallIcon(R.mipmap.ic_launcher)
                            .setContentTitle("Don't miss")
                            .setContentText("Tomorrow is departure to ${it.departure.city}")
                            .build())
                }
                .subscribe()
    }

    /**
     * @return true if the supplied when is today else false
     */
    private fun isTomorrow(`when`: Long): Boolean {
        val time = Time()
        time.set(`when`)
        val thenYear = time.year
        val thenMonth = time.month
        val thenMonthDay = time.monthDay
        time.set(System.currentTimeMillis())
        return thenYear == time.year
                && thenMonth == time.month
                && thenMonthDay == time.monthDay + 1
    }
}
