package io.codetail.airplanes.screen

import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.databinding.ViewDataBinding
import android.graphics.Canvas
import android.os.Bundle
import android.support.transition.Scene
import android.support.transition.TransitionManager
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.View.NO_ID
import android.view.ViewGroup
import io.codetail.airplanes.R
import io.codetail.airplanes.databinding.*
import io.codetail.airplanes.domain.Ticket
import io.codetail.airplanes.domain.UserInterestType
import io.codetail.airplanes.ext.dp
import io.codetail.airplanes.ext.get
import io.codetail.airplanes.global.BaseAdapter
import io.codetail.airplanes.global.BindingLifecycleFragment
import io.codetail.airplanes.global.Globals
import io.codetail.airplanes.global.ObserverAdapter
import io.codetail.airplanes.screen.detail.TicketDetailsActivity
import io.reactivex.Flowable
import timber.log.Timber

/**
 * created at 5/20/17
 *
 * @author Ozodrukh
 * @version 1.0
 */
class TicketsViewFragment : BindingLifecycleFragment<FragmentToursBinding>() {
    enum class DataType {
        LIVE {
            override fun getTickets(viewModel: TicketsViewModel): Flowable<List<Any>> {
                return viewModel.liveTicketsByDate()
            }
        },

        INTERESTED {
            override fun getTickets(viewModel: TicketsViewModel): Flowable<List<Any>> {
                return viewModel.ticketsByDate(UserInterestType.INTERESTED)
            }
        },

        GOING {
            override fun getTickets(viewModel: TicketsViewModel): Flowable<List<Any>> {
                return viewModel.ticketsByDate(UserInterestType.GOING)
            }
        },

        NO_TICKETS {
            override fun getTickets(viewModel: TicketsViewModel): Flowable<List<Any>> {
                return Flowable.empty()
            }
        };

        abstract fun getTickets(viewModel: TicketsViewModel): Flowable<List<Any>>;
    }

    override val layoutId: Int = R.layout.fragment_tours

    val dataTypeName by lazyArgument<String>("dataType")
    val dataType: DataType
        get() = DataType.valueOf(dataTypeName)

    val viewModel by lazy {
        ViewModelProviders.of(this).get<TicketsViewModel>()
                .apply { Timber.d("Instance hash: $this") }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.d(arguments.toString())

        val adapter = BaseAdapter<Any, ItemTicketOrdinalBinding>(lifecycle, dataType.getTickets(viewModel))

        adapter.add(object : BaseAdapter.BindingViewFactory<String, ItemTicketDateGroupBinding> {
            override val viewType: Int = R.layout.item_ticket_date_group

            override fun supports(adapter: BaseAdapter<*, *>, position: Int, item: Any): Boolean {
                return item is String
            }

            override fun bind(component: ItemTicketDateGroupBinding, data: String, position: Int) {
                component.departure = data
            }

        })

        adapter.add(object : BaseAdapter.BindingViewFactory<Ticket, ItemTicketBaseBinding> {
            override val viewType: Int = R.layout.item_ticket_base

            var expandedPosition: Int = NO_ID

            override fun supports(adapter: BaseAdapter<*, *>, position: Int, item: Any): Boolean {
                return item is Ticket
            }

            override fun createViewComponent(inflater: LayoutInflater, parent: ViewGroup): ItemTicketBaseBinding {
                val component = super.createViewComponent(inflater, parent)
                val childComponent = ItemTicketOrdinalBinding.inflate(inflater, component.sceneRoot, false)
                component.sceneRoot.addView(childComponent.root)
                component.sceneRoot.setTag(childComponent)
                return component
            }

            fun bind(childComponent: ViewDataBinding, data: Ticket) {
                if (childComponent is ItemTicketOrdinalBinding) {
                    childComponent.ticket = data
                } else if (childComponent is ItemTicketOrdinalExpandedBinding) {
                    childComponent.ticket = data
                    childComponent.actionGoing.setOnClickListener {
                        viewModel.setUserInterestType(data, UserInterestType.GOING)
                    }
                    childComponent.actionInterested.setOnClickListener {
                        viewModel.setUserInterestType(data, UserInterestType.INTERESTED)
                    }
                }
            }

            fun collapseExpandedComponent(content: RecyclerView, factory: LayoutInflater) {
                val adapter = (content.adapter as ObserverAdapter<*, *>)
                if (expandedPosition >= adapter.itemCount) {
                    return
                }

                val holder = content.findViewHolderForAdapterPosition(expandedPosition)
                val ticket = adapter.getItem(expandedPosition) as Ticket

                @Suppress("unchecked_cast")
                holder as BaseAdapter.DataBindingHolder<Ticket, ItemTicketBaseBinding>

                val childComponent = ItemTicketOrdinalBinding.inflate(factory,
                        holder.viewComponent.sceneRoot, false)

                bind(childComponent, ticket)
                holder.viewComponent.executePendingBindings()
                holder.viewComponent.sceneRoot.cardElevation = 2.dp(content.context).toFloat()
                holder.viewComponent.sceneRoot.tag = childComponent

                TransitionManager.go(Scene(holder.viewComponent.sceneRoot, childComponent.root))
            }

            override fun bind(component: ItemTicketBaseBinding, data: Ticket, position: Int) {
                val childComponent = component.sceneRoot.tag as ViewDataBinding;
                bind(childComponent, data)

                component.root.setOnLongClickListener {
                    context.startActivity(Intent(context, TicketDetailsActivity::class.java))
                    return@setOnLongClickListener true
                }

                component.root.setOnClickListener {
                    val parent = it as ViewGroup
                    val factory = LayoutInflater.from(parent.context)
                    val transitingComponent: ViewDataBinding =
                            if (expandedPosition != position) {
                                ItemTicketOrdinalExpandedBinding.inflate(factory, parent, false)
                            } else {
                                ItemTicketOrdinalBinding.inflate(factory, parent, false)
                            }

                    if (expandedPosition == position) {
                        expandedPosition = NO_ID
                        component.sceneRoot.cardElevation = 2.dp(parent.context).toFloat()
                    } else {
                        if (expandedPosition != NO_ID) {
                            collapseExpandedComponent(component.root.parent as RecyclerView, factory)
                        }

                        expandedPosition = position
                        component.sceneRoot.cardElevation = 8.dp(parent.context).toFloat()
                    }

                    bind(transitingComponent, data)

                    transitingComponent.executePendingBindings()
                    component.sceneRoot.tag = childComponent

                    val expandScene = Scene(component.root as ViewGroup, transitingComponent.root)
                    TransitionManager.go(expandScene)
                }
            }
        })

        viewComponent.container.setAdapter(adapter)
        viewComponent.container.setLayoutManager(object : LinearLayoutManager(context) {
            override fun supportsPredictiveItemAnimations(): Boolean {
                return false
            }
        })
        viewComponent.container.setItemAnimator(DefaultItemAnimator())
        viewComponent.container.addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
                for (index in 0.until(parent.childCount)) {
                    val child = parent.getChildAt(index)
                    val adapterPos = parent.getChildAdapterPosition(child)
                    if (0 > adapterPos || adapterPos >= adapter.itemCount) {
                        break;
                    }

                    val item = adapter.getItem(adapterPos)

                    if (item is Ticket)
                        c.drawLine(
                                child.left.toFloat(), child.top.toFloat(),
                                child.right.toFloat(), child.top.toFloat(),
                                Globals.dividerPaint
                        )
                }
            }
        })
    }


}
