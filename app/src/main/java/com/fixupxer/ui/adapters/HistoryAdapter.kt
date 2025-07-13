package com.fixupxer.ui.adapters

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.fixupxer.R
import com.fixupxer.databinding.ItemHistoryBinding
import com.fixupxer.domain.model.UrlHistory
import android.view.View

/**
 * Adapter for displaying URL history items
 */
class HistoryAdapter(
    private val onItemClick: (UrlHistory) -> Unit,
    private val onItemDelete: (UrlHistory) -> Unit
) : ListAdapter<UrlHistory, HistoryAdapter.HistoryViewHolder>(HistoryDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val binding = ItemHistoryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return HistoryViewHolder(binding, onItemClick, onItemDelete)
    }
    
    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    /**
     * ViewHolder for history items
     */
    class HistoryViewHolder(
        private val binding: ItemHistoryBinding,
        private val onItemClick: (UrlHistory) -> Unit,
        private val onItemDelete: (UrlHistory) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(item: UrlHistory) {
            binding.apply {
                // Original URL
                textViewOriginalUrl.text = item.originalUrl
                
                // Processed URL (cleaned URL)
                textViewProcessedUrl.text = item.cleanedUrl
                
                // Timestamp
                textViewTimestamp.text = item.timeAgo
                
                // Platform - show if available
                if (item.platform != "Other") {
                    textViewPlatform.visibility = View.VISIBLE
                    textViewPlatform.text = itemView.context.getString(R.string.platform_label, item.platform)
                } else {
                    textViewPlatform.visibility = View.GONE
                }
                
                // Copy button
                buttonCopy.setOnClickListener {
                    val clipboard = binding.root.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("FixupXer URL", item.cleanedUrl)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(binding.root.context, R.string.url_copied, Toast.LENGTH_SHORT).show()
                }
                
                // Share button
                buttonShare.setOnClickListener {
                    val shareIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, item.cleanedUrl)
                        type = "text/plain"
                    }
                    binding.root.context.startActivity(Intent.createChooser(shareIntent, binding.root.context.getString(R.string.share_via)))
                }
                
                // Long press to delete
                root.setOnLongClickListener {
                    onItemDelete(item)
                    true
                }
                
                // Click listener
                root.setOnClickListener {
                    onItemClick(item)
                }
            }
        }
    }
    
    /**
     * DiffUtil callback for efficient updates
     */
    class HistoryDiffCallback : DiffUtil.ItemCallback<UrlHistory>() {
        override fun areItemsTheSame(oldItem: UrlHistory, newItem: UrlHistory): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: UrlHistory, newItem: UrlHistory): Boolean {
            return oldItem == newItem
        }
    }
} 