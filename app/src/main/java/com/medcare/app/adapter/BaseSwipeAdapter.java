package com.medcare.app.adapter;

import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

public abstract class BaseSwipeAdapter<VH extends BaseSwipeAdapter.SwipeableViewHolder>
        extends RecyclerView.Adapter<VH> {

    private int previouslyRevealed = -1;

    public void closeRevealed() {
        if (previouslyRevealed != -1) {
            int pos = previouslyRevealed;
            previouslyRevealed = -1;
            notifyItemChanged(pos);
        }
    }

    protected void resetRevealed() {
        previouslyRevealed = -1;
    }

    public abstract static class SwipeableViewHolder extends RecyclerView.ViewHolder {
        protected MaterialCardView cardView;
        protected View deleteAction;
        protected int deleteActionWidth;
        protected float startX;
        protected boolean isRevealed;
        protected int touchSlop;
        private BaseSwipeAdapter adapter;

        public SwipeableViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(com.medcare.app.R.id.card_content);
            deleteAction = itemView.findViewById(com.medcare.app.R.id.delete_action);
            deleteActionWidth = (int) (96 * itemView.getResources().getDisplayMetrics().density);
            touchSlop = ViewConfiguration.get(itemView.getContext()).getScaledTouchSlop();

            deleteAction.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && adapter != null) {
                    isRevealed = false;
                    cardView.animate().translationX(0).setDuration(200).start();
                    onDeleteActionClick(position);
                }
            });

            cardView.setOnTouchListener((v, event) -> {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        startX = event.getRawX();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        float dx = event.getRawX() - startX;
                        if (Math.abs(dx) > touchSlop) {
                            float maxReveal = deleteActionWidth;
                            float offset = isRevealed ? -maxReveal : 0;
                            float translation = Math.max(-maxReveal, Math.min(0, dx + offset));
                            cardView.setTranslationX(translation);
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                        handleActionUp(event);
                        return true;
                }
                return false;
            });
        }

        void bindSwipeState(BaseSwipeAdapter adapter, int position) {
            this.adapter = adapter;
            if (adapter.previouslyRevealed == position && deleteActionWidth > 0) {
                cardView.setTranslationX(-deleteActionWidth);
                isRevealed = true;
            } else {
                cardView.setTranslationX(0);
                isRevealed = false;
            }
        }

        private void handleActionUp(MotionEvent event) {
            float totalDx = event.getRawX() - startX;
            float currentTranslation = cardView.getTranslationX();
            if (Math.abs(totalDx) < touchSlop) {
                if (isRevealed) {
                    snapCard(0, false);
                } else {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION && adapter != null) {
                        onItemClick(position);
                    }
                }
            } else {
                if (isRevealed) {
                    if (totalDx > 20) {
                        snapCard(0, false);
                    } else {
                        snapCard(-deleteActionWidth, true);
                    }
                } else {
                    if (currentTranslation < -deleteActionWidth * 0.4f) {
                        snapCard(-deleteActionWidth, true);
                        if (adapter != null && adapter.previouslyRevealed != -1 && adapter.previouslyRevealed != getAdapterPosition()) {
                            adapter.notifyItemChanged(adapter.previouslyRevealed);
                        }
                        if (adapter != null) {
                            adapter.previouslyRevealed = getAdapterPosition();
                        }
                    } else {
                        snapCard(0, false);
                    }
                }
            }
        }

        private void snapCard(float targetX, boolean revealed) {
            isRevealed = revealed;
            if (!revealed && adapter != null && adapter.previouslyRevealed == getAdapterPosition()) {
                adapter.previouslyRevealed = -1;
            }
            cardView.animate().translationX(targetX).setDuration(200).start();
        }

        protected abstract void onItemClick(int position);
        protected abstract void onDeleteActionClick(int position);
    }
}
