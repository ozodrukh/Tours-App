package io.codetail.airplanes.screen.tickets

import android.view.View.NO_ID
import android.view.ViewGroup
import io.codetail.airplanes.databinding.*
import io.codetail.airplanes.domain.Ticket
import io.codetail.airplanes.ext.dp
import io.codetail.airplanes.ext.get
import io.codetail.airplanes.screen.detail.TicketDetailsActivity

/**
 * created at 5/20/17
 *
 * @author Ozodrukh
 * @version 1.0
 */
class TicketsViewFragment : io.codetail.airplanes.global.BindingLifecycleFragment<FragmentToursBinding>() {
    enum class DataType {
        LIVE {
            override fun getTickets(viewModel: TicketsViewModel): io.reactivex.Flowable<List<Any>> {
                return viewModel.liveTicketsByDate()
            }
        },

        INTERESTED {
            override fun getTickets(viewModel: TicketsViewModel): io.reactivex.Flowable<List<Any>> {
                return viewModel.ticketsByDate(io.codetail.airplanes.domain.UserInterestType.INTERESTED)
            }
        },

        GOING {
            override fun getTickets(viewModel: TicketsViewModel): io.reactivex.Flowable<List<Any>> {
                return viewModel.ticketsByDate(io.codetail.airplanes.domain.UserInterestType.GOING)
            }
        },

        NO_TICKETS {
            override fun getTickets(viewModel: TicketsViewModel): io.reactivex.Flowable<List<Any>> {
                return io.reactivex.Flowable.empty()
            }
        };

        abstract fun getTickets(viewModel: TicketsViewModel): io.reactivex.Flowable<List<Any>>;
    }

    override val layoutId: Int = io.codetail.airplanes.R.layout.fragment_tours

    val dataTypeName by lazyArgument<String>("dataType")
    val dataType: io.codetail.airplanes.screen.tickets.TicketsViewFragment.DataType
        get() = io.codetail.airplanes.screen.tickets.TicketsViewFragment.DataType.valueOf(dataTypeName)

    val viewModel by lazy {
        android.arch.lifecycle.ViewModelProviders.of(this).get<TicketsViewModel>()
                .apply { timber.log.Timber.d("Instance hash: $this") }
    }

    override fun onViewCreated(view: android.view.View, savedInstanceState: android.os.Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        timber.log.Timber.d(arguments.toString())

        val adapter = io.codetail.airplanes.global.BaseAdapter<Any, ItemTicketOrdinalBinding>(lifecycle, dataType.getTickets(viewModel))

        adapter.add(object : io.codetail.airplanes.global.BaseAdapter.BindingViewFactory<String, ItemTicketDateGroupBinding> {
            override val viewType: Int = io.codetail.airplanes.R.layout.item_ticket_date_group

            override fun supports(adapter: io.codetail.airplanes.global.BaseAdapter<*, *>, position: Int, item: Any): Boolean {
                return item is String
            }

            override fun bind(component: io.codetail.airplanes.databinding.ItemTicketDateGroupBinding, data: String, position: Int) {
                component.departure = data
            }

        })

        adapter.add(object : io.codetail.airplanes.global.BaseAdapter.BindingViewFactory<Ticket, ItemTicketBaseBinding> {
            override val viewType: Int = io.codetail.airplanes.R.layout.item_ticket_base

            var expandedPosition: Int = NO_ID

            override fun supports(adapter: io.codetail.airplanes.global.BaseAdapter<*, *>, position: Int, item: Any): Boolean {
                return item is io.codetail.airplanes.domain.Ticket
            }

            override fun createViewComponent(inflater: android.view.LayoutInflater, parent: android.view.ViewGroup): io.codetail.airplanes.databinding.ItemTicketBaseBinding {
                val component = super.createViewComponent(inflater, parent)
                val childComponent = io.codetail.airplanes.databinding.ItemTicketOrdinalBinding.inflate(inflater, component.sceneRoot, false)
                component.sceneRoot.addView(childComponent.root)
                component.sceneRoot.setTag(childComponent)
                return component
            }

            fun bind(childComponent: android.databinding.ViewDataBinding, data: io.codetail.airplanes.domain.Ticket) {
                if (childComponent is io.codetail.airplanes.databinding.ItemTicketOrdinalBinding) {
                    childComponent.ticket = data
                } else if (childComponent is io.codetail.airplanes.databinding.ItemTicketOrdinalExpandedBinding) {
                    childComponent.ticket = data
                    childComponent.actionGoing.setOnClickListener {
                        viewModel.setUserInterestType(data, io.codetail.airplanes.domain.UserInterestType.GOING)
                    }
                    childComponent.actionInterested.setOnClickListener {
                        viewModel.setUserInterestType(data, io.codetail.airplanes.domain.UserInterestType.INTERESTED)
                    }
                }
            }

            fun collapseExpandedComponent(content: android.support.v7.widget.RecyclerView, factory: android.view.LayoutInflater) {
                val adapter = (content.adapter as io.codetail.airplanes.global.ObserverAdapter<*, *>)
                if (expandedPosition >= adapter.itemCount) {
                    return
                }

                val holder = content.findViewHolderForAdapterPosition(expandedPosition)
                val ticket = adapter.getItem(expandedPosition) as io.codetail.airplanes.domain.Ticket

                @Suppress("unchecked_cast")
                holder as io.codetail.airplanes.global.BaseAdapter.DataBindingHolder<Ticket, ItemTicketBaseBinding>

                val childComponent = io.codetail.airplanes.databinding.ItemTicketOrdinalBinding.inflate(factory,
                        holder.viewComponent.sceneRoot, false)

                bind(childComponent, ticket)
                holder.viewComponent.executePendingBindings()
                holder.viewComponent.sceneRoot.cardElevation = 2.dp(content.context).toFloat()
                holder.viewComponent.sceneRoot.tag = childComponent

                android.support.transition.TransitionManager.go(android.support.transition.Scene(holder.viewComponent.sceneRoot, childComponent.root))
            }

            override fun bind(component: io.codetail.airplanes.databinding.ItemTicketBaseBinding, data: io.codetail.airplanes.domain.Ticket, position: Int) {
                val childComponent = component.sceneRoot.tag as android.databinding.ViewDataBinding;
                bind(childComponent, data)

                component.root.setOnLongClickListener {
                    context.startActivity(android.content.Intent(context, TicketDetailsActivity::class.java))
                    return@setOnLongClickListener true
                }

                component.root.setOnClickListener {
                    val parent = it as android.view.ViewGroup
                    val factory = android.view.LayoutInflater.from(parent.context)
                    val transitingComponent: android.databinding.ViewDataBinding =
                            if (expandedPosition != position) {
                                io.codetail.airplanes.databinding.ItemTicketOrdinalExpandedBinding.inflate(factory, parent, false)
                            } else {
                                io.codetail.airplanes.databinding.ItemTicketOrdinalBinding.inflate(factory, parent, false)
                            }

                    if (expandedPosition == position) {
                        expandedPosition = NO_ID
                        component.sceneRoot.cardElevation = 2.dp(parent.context).toFloat()
                    } else {
                        if (expandedPosition != NO_ID) {
                            collapseExpandedComponent(component.root.parent as android.support.v7.widget.RecyclerView, factory)
                        }

                        expandedPosition = position
                        component.sceneRoot.cardElevation = 8.dp(parent.context).toFloat()
                    }

                    bind(transitingComponent, data)

                    transitingComponent.executePendingBindings()
                    component.sceneRoot.tag = childComponent

                    val expandScene = android.support.transition.Scene(component.root as ViewGroup, transitingComponent.root)
                    android.support.transition.TransitionManager.go(expandScene)
                }
            }
        })

        viewComponent.container.setAdapter(adapter)
        viewComponent.container.setLayoutManager(object : android.support.v7.widget.LinearLayoutManager(context) {
            override fun supportsPredictiveItemAnimations(): Boolean {
                return false
            }
        })
        viewComponent.container.setItemAnimator(android.support.v7.widget.DefaultItemAnimator())
        viewComponent.container.addItemDecoration(object : android.support.v7.widget.RecyclerView.ItemDecoration() {
            override fun onDrawOver(c: android.graphics.Canvas, parent: android.support.v7.widget.RecyclerView, state: android.support.v7.widget.RecyclerView.State) {
                for (index in 0.until(parent.childCount)) {
                    val child = parent.getChildAt(index)
                    val adapterPos = parent.getChildAdapterPosition(child)
                    if (0 > adapterPos || adapterPos >= adapter.itemCount) {
                        break;
                    }

                    val item = adapter.getItem(adapterPos)

                    if (item is io.codetail.airplanes.domain.Ticket)
                        c.drawLine(
                                child.left.toFloat(), child.top.toFloat(),
                                child.right.toFloat(), child.top.toFloat(),
                                io.codetail.airplanes.global.Globals.dividerPaint
                        )
                }
            }
        })
    }


}
