package com.tbuonomo.viewpagerdotsindicator;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.graphics.drawable.GradientDrawable;
import android.support.animation.FloatPropertyCompat;
import android.support.animation.SpringAnimation;
import android.support.animation.SpringForce;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.PagerSnapHelper;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SnapHelper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import java.util.ArrayList;
import java.util.List;

import static android.widget.LinearLayout.HORIZONTAL;
import static com.tbuonomo.viewpagerdotsindicator.UiUtils.getThemePrimaryColor;

public class WormDotsIndicator extends FrameLayout {

    private List<ImageView> strokeDots;
    private ImageView dotIndicatorView;
    private View dotIndicatorLayout;
    private ViewPager viewPager;
    private RecyclerView recyclerView = new RecyclerView(getContext());
    private SnapHelper snapHelper = new PagerSnapHelper();

    // Attributes
    private int dotIndicatorSize;
    private int dotsSize;
    private int dotsSpacing;
    private int dotsStrokeWidth;
    private int dotsCornerRadius;
    private int dotIndicatorColor;
    private int dotsStrokeColor;

    private int horizontalMargin;
    private SpringAnimation dotIndicatorXSpring;
    private SpringAnimation dotIndicatorWidthSpring;
    private LinearLayout strokeDotsLinearLayout;

    private boolean dotsClickable;
    private ViewPager.OnPageChangeListener pageChangedListener;
    private RecyclerView.OnScrollListener recyclerPagerListener;

    private List<Integer> colors;

    public WormDotsIndicator(Context context) {
        this(context, null);
    }

    public WormDotsIndicator(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WormDotsIndicator(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        strokeDots = new ArrayList<>();
        strokeDotsLinearLayout = new LinearLayout(context);
        LayoutParams linearParams = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        horizontalMargin = dpToPx(24);
        linearParams.setMargins(horizontalMargin, 8, horizontalMargin, 8);
        strokeDotsLinearLayout.setLayoutParams(linearParams);
        strokeDotsLinearLayout.setOrientation(HORIZONTAL);
        addView(strokeDotsLinearLayout);

        dotIndicatorSize = dpToPx(22); // 16dp
        dotsSpacing = dpToPx(4); // 4dp
        dotsStrokeWidth = dpToPx(2); // 2dp
        dotsCornerRadius = dotsSize / 2;
        dotIndicatorColor = getThemePrimaryColor(context);
        dotsStrokeColor = dotIndicatorColor;
        dotsClickable = true;

        if (attrs != null) {
            TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.WormDotsIndicator);

            // Dots attributes
            dotIndicatorColor = a.getColor(R.styleable.WormDotsIndicator_dotsColor, dotIndicatorColor);
            dotsStrokeColor = a.getColor(R.styleable.WormDotsIndicator_dotsStrokeColor, dotIndicatorColor);
            dotIndicatorSize = (int) a.getDimension(R.styleable.WormDotsIndicator_dotsSize, dotIndicatorSize);
            dotsSpacing = (int) a.getDimension(R.styleable.WormDotsIndicator_dotsSpacing, dotsSpacing);
            dotsCornerRadius = (int) a.getDimension(R.styleable.WormDotsIndicator_dotsCornerRadius, dotsSize / 2);

            // Spring dots attributes
            dotsStrokeWidth = (int) a.getDimension(R.styleable.WormDotsIndicator_dotsStrokeWidth, dotsStrokeWidth);

            a.recycle();
        }

        dotsSize = (int) (dotIndicatorSize * 0.7);

        if (isInEditMode()) {
            addPageDots(5);
            addView(buildDot(false, 0));
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        refreshDots();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, (int) (getMeasuredHeight() * 1.5));
    }

    private void refreshDots() {
        if (dotIndicatorLayout == null) {
            setUpDotIndicator();
        }

        if (viewPager != null && viewPager.getAdapter() != null) {
            // Check if we need to refresh the strokeDots count
            if (strokeDots.size() < viewPager.getAdapter().getCount()) {
                addPageDots(viewPager.getAdapter().getCount() - strokeDots.size());
            } else if (strokeDots.size() > viewPager.getAdapter().getCount()) {
                removeDots(strokeDots.size() - viewPager.getAdapter().getCount());
            }
            setUpDotsAnimators();

        } else if (recyclerView != null && recyclerView.getAdapter() != null) {
            if (strokeDots.size() < recyclerView.getAdapter().getItemCount()) {
                addPageDots(recyclerView.getAdapter().getItemCount() - strokeDots.size());
            } else if (strokeDots.size() > recyclerView.getAdapter().getItemCount()) {
                removeDots(strokeDots.size() - recyclerView.getAdapter().getItemCount());
            }
            setUpDotsAnimators();

        } else {
            Log.e(WormDotsIndicator.class.getSimpleName(), "You have to set an adapter to the view pager before !");
        }
    }

    private void setUpDotIndicator() {
        if ((viewPager != null && viewPager.getAdapter() != null && viewPager.getAdapter().getCount() == 0) ||
                (recyclerView != null && recyclerView.getAdapter() != null && recyclerView.getAdapter().getItemCount() == 0)) {
            return;
        }

        if (dotIndicatorView != null && indexOfChild(dotIndicatorView) != -1) {
            removeView(dotIndicatorView);
        }

        dotIndicatorLayout = buildDot(true, 0);
        dotIndicatorView = dotIndicatorLayout.findViewById(R.id.worm_dot);
        addView(dotIndicatorLayout);
        dotIndicatorXSpring = new SpringAnimation(dotIndicatorLayout, SpringAnimation.TRANSLATION_X);
        SpringForce springForceX = new SpringForce(0);
        springForceX.setDampingRatio(1f);
        springForceX.setStiffness(300);
        dotIndicatorXSpring.setSpring(springForceX);

        FloatPropertyCompat floatPropertyCompat = new FloatPropertyCompat("DotsWidth") {
            @Override
            public float getValue(Object object) {
                return dotIndicatorView.getLayoutParams().width;
            }

            @Override
            public void setValue(Object object, float value) {
                ViewGroup.LayoutParams params = dotIndicatorView.getLayoutParams();
                params.width = (int) value;
                dotIndicatorView.requestLayout();
            }
        };
        dotIndicatorWidthSpring = new SpringAnimation(dotIndicatorLayout, floatPropertyCompat);
        SpringForce springForceWidth = new SpringForce(0);
        springForceWidth.setDampingRatio(1f);
        springForceWidth.setStiffness(300);
        dotIndicatorWidthSpring.setSpring(springForceWidth);
    }

    private void addPageDots(int count) {
        for (int i = 0; i < count; i++) {
            View dot = buildDot(false, i);
            final int finalI = i;
            dot.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (dotsClickable && viewPager != null && viewPager.getAdapter() != null && finalI < viewPager.getAdapter()
                                                                                                                  .getCount()) {
                        viewPager.setCurrentItem(finalI, true);
                    }
                }
            });

            ImageView ivDot = dot.findViewById(R.id.worm_dot);
            strokeDots.add(ivDot);
            strokeDotsLinearLayout.addView(dot);
        }
    }

    private View buildDot(boolean indicator, int position) {
        RelativeLayout dot = (RelativeLayout) LayoutInflater.from(getContext())
                                                            .inflate(
                                                                    indicator ? R.layout.worm_dot_layout_indicator : R.layout.worm_dot_layout,
                                                                    this, false);

        View dotImageView = dot.findViewById(R.id.worm_dot);
        dotImageView.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.worm_dot_background));
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) dotImageView.getLayoutParams();

        if (!indicator) {
            params.width = (int) (dotsSize * 0.7);
            params.height = (int) (dotsSize * 0.7);
        } else {
            params.width = dotsSize;
            params.height = dotsSize;
        }

        params.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);

        params.setMargins(dotsSpacing, 0, dotsSpacing, 0);

        GradientDrawable dotBackground = (GradientDrawable) dotImageView.getBackground();
        dotBackground.setColor(dotIndicatorColor);
        dotBackground.setCornerRadius(5000);

        if (!indicator && colors != null && !colors.isEmpty()) {
            dotBackground.setColor(colors.get(position % colors.size()));
        }

        return dot;
    }

    private void removeDots(int count) {
        for (int i = 0; i < count; i++) {
            strokeDotsLinearLayout.removeViewAt(strokeDotsLinearLayout.getChildCount() - 1);
            strokeDots.remove(strokeDots.size() - 1);
        }
    }

    private int currentSnapPosition = 0;

    private void setUpDotsAnimators() {
        if (viewPager != null && viewPager.getAdapter() != null && viewPager.getAdapter().getCount() > 0) {
            if (pageChangedListener != null) {
                viewPager.removeOnPageChangeListener(pageChangedListener);
            }
            setUpOnPageChangedListener();
            viewPager.addOnPageChangeListener(pageChangedListener);
            pageChangedListener.onPageScrolled(0, 0, 0);
        } else if (recyclerView != null && recyclerView.getAdapter() != null && recyclerView.getAdapter().getItemCount() > 0) {
            if (recyclerPagerListener != null) {
                recyclerView.removeOnScrollListener(recyclerPagerListener);
            }
            setUpRecyclerPageListener();
            recyclerView.addOnScrollListener(recyclerPagerListener);
        }
    }

    private void setUpRecyclerPageListener() {
        recyclerPagerListener = new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                int snapPosition = recyclerView.getLayoutManager()
                                               .getPosition(snapHelper.findSnapView(recyclerView.getLayoutManager()));

                float xFinalPosition = ((View) strokeDots.get(snapPosition).getParent()).getX() + dotIndicatorSize;
                float widthFinalPosition = dotsSize + (Math.abs(dx) / 3f);

                if (dotIndicatorXSpring.getSpring().getFinalPosition() != xFinalPosition) {
                    dotIndicatorXSpring.getSpring().setFinalPosition(xFinalPosition);
                    if (colors != null && !colors.isEmpty()) {

                        ValueAnimator animator = ValueAnimator.ofArgb(colors.get(currentSnapPosition % colors.size()),
                                                                      colors.get(snapPosition % colors.size()));

                        animator.setDuration(300);
                        //animator.setInterpolator(new LinearInterpolator());
                        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                            @Override
                            public void onAnimationUpdate(ValueAnimator animation) {
                                ((GradientDrawable) dotIndicatorView.getBackground()).setColor(
                                        (Integer) animation.getAnimatedValue());
                            }
                        });
                        animator.start();
                    }
                }

                currentSnapPosition = snapPosition;

                if (dotIndicatorWidthSpring.getSpring().getFinalPosition() != widthFinalPosition) {
                    dotIndicatorWidthSpring.getSpring().setFinalPosition(widthFinalPosition);
                }

                if (!dotIndicatorXSpring.isRunning()) {
                    dotIndicatorXSpring.start();
                }

                if (!dotIndicatorWidthSpring.isRunning()) {
                    dotIndicatorWidthSpring.start();
                }
            }

            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    dotIndicatorWidthSpring.getSpring().setFinalPosition(dotsSize);
                }
            }
        };

    }

    private void setUpOnPageChangedListener() {
        pageChangedListener = new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                int stepX = dotsSize + dotsSpacing * 2;
                float xFinalPosition;
                float widthFinalPosition;

                if (positionOffset >= 0 && positionOffset < 0.1f) {
                    xFinalPosition = horizontalMargin + position * stepX;
                    widthFinalPosition = dotsSize;
                } else if (positionOffset >= 0.1f && positionOffset <= 0.9f) {
                    xFinalPosition = horizontalMargin + position * stepX;
                    widthFinalPosition = dotsSize + stepX;
                } else {
                    xFinalPosition = horizontalMargin + (position + 1) * stepX;
                    widthFinalPosition = dotsSize;
                }

                if (dotIndicatorXSpring.getSpring().getFinalPosition() != xFinalPosition) {
                    dotIndicatorXSpring.getSpring().setFinalPosition(xFinalPosition);
                }

                if (dotIndicatorWidthSpring.getSpring().getFinalPosition() != widthFinalPosition) {
                    dotIndicatorWidthSpring.getSpring().setFinalPosition(widthFinalPosition);
                }

                if (!dotIndicatorXSpring.isRunning()) {
                    dotIndicatorXSpring.start();
                }

                if (!dotIndicatorWidthSpring.isRunning()) {
                    dotIndicatorWidthSpring.start();
                }
            }

            @Override
            public void onPageSelected(int position) {
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        };
    }

    private void setUpViewPager() {
        if (viewPager.getAdapter() != null) {
            viewPager.getAdapter().registerDataSetObserver(new DataSetObserver() {
                @Override
                public void onChanged() {
                    super.onChanged();
                    refreshDots();
                }
            });
        }
    }

    private int dpToPx(int dp) {
        return (int) (getContext().getResources().getDisplayMetrics().density * dp);
    }

    public void setViewPager(ViewPager viewPager) {
        this.viewPager = viewPager;
        setUpViewPager();
        refreshDots();
    }

    public void setRecyclerView(RecyclerView recyclerView, SnapHelper snapHelper) {
        this.recyclerView = recyclerView;
        this.snapHelper = snapHelper;
        recyclerView.getAdapter().registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                refreshDots();
            }
        });
        refreshDots();
    }

    public void setColorsList(List<Integer> colors) {
        this.colors = colors;
    }

}
