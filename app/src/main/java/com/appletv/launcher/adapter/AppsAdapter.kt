package com.appletv.launcher.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.appletv.launcher.R
import com.appletv.launcher.data.AppModel
import com.appletv.launcher.util.TvFocusHelper

class AppsAdapter(
    private val onAppClick: (AppModel) -> Unit,
    private val onAppFocused: (AppModel, View) -> Unit,
    private val onAppLongClick: (AppModel) -> Unit
) : ListAdapter<AppModel, AppsAdapter.AppViewHolder>(AppDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app, parent, false)
        return AppViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class AppViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivIcon: ImageView = itemView.findViewById(R.id.iv_app_icon)
        private val tvTitle: TextView = itemView.findViewById(R.id.tv_app_title)
        private val cardContainer: View = itemView.findViewById(R.id.card_container)
        private val ivBadge: ImageView = itemView.findViewById(R.id.iv_favorite_badge)
        private val specularHighlight: View = itemView.findViewById(R.id.view_specular_highlight)

        fun bind(app: AppModel) {
            tvTitle.text = app.label
            
            // استخدام الأيقونة الأصلية المستخرجة من شاشة التلفزيون بدقة كاملة
            if (app.iconBitmap != null) {
                ivIcon.setImageBitmap(app.iconBitmap)
            } else if (app.iconDrawable != null) {
                ivIcon.setImageDrawable(app.iconDrawable)
            } else {
                ivIcon.setImageResource(R.mipmap.ic_launcher)
            }

            ivBadge.visibility = if (app.isFavorite) View.VISIBLE else View.GONE

            // تمكين استلام التركيز من ريموت التلفزيون
            itemView.isFocusable = true
            itemView.isFocusableInTouchMode = true

            itemView.setOnFocusChangeListener { view, hasFocus ->
                // تأثير تكبير Apple TV ثلاثي الأبعاد الأنيق (1.15x) مع ظل مضيء
                TvFocusHelper.animateFocusState(
                    cardContainer = cardContainer,
                    titleView = tvTitle,
                    specularHighlight = specularHighlight,
                    hasFocus = hasFocus
                )
                if (hasFocus) {
                    onAppFocused(app, view)
                }
            }

            itemView.setOnClickListener {
                // تأثير النبضة عند الضغط
                TvFocusHelper.animateClickPulse(cardContainer) {
                    onAppClick(app)
                }
            }

            itemView.setOnLongClickListener {
                onAppLongClick(app)
                true
            }
        }
    }

    class AppDiffCallback : DiffUtil.ItemCallback<AppModel>() {
        override fun areItemsTheSame(oldItem: AppModel, newItem: AppModel): Boolean {
            return oldItem.packageName == newItem.packageName
        }

        override fun areContentsTheSame(oldItem: AppModel, newItem: AppModel): Boolean {
            return oldItem == newItem
        }
    }
}
