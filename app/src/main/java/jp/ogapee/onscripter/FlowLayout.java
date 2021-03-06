package jp.ogapee.onscripter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

public class FlowLayout
        extends ViewGroup
{
    
    private boolean mIsFirstNoLeftPadding = false; // 第一个是否有左padding
    private boolean mIsFirstLineNoTopPadding = false; // 第一行是否有top
    private int mFirstLineTopHeight = 0;
    
    /**
     * 存储所有的View
     */
    private List<List<View>> mAllViews = new ArrayList<List<View>>();
    /**
     * 每一行的高度
     */
    private List<Integer> mLineHeight = new ArrayList<Integer>();
    
    public FlowLayout(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
    }
    
    public FlowLayout(Context context, AttributeSet attrs)
    {
        this(context, attrs, 0);
    }
    
    public FlowLayout(Context context)
    {
        this(context, null);
    }
    
    /**
     * 这个方法的作用是:
     * 1.测量子View的宽和高
     * 2.设置自己的宽和高
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        int sizeWidth = MeasureSpec.getSize(widthMeasureSpec);
        int modeWidth = MeasureSpec.getMode(widthMeasureSpec);
        int sizeHeight = MeasureSpec.getSize(heightMeasureSpec);
        int modeHeight = MeasureSpec.getMode(heightMeasureSpec);
        
        // wrap_content
        int width = 0;
        int height = 0;
        
        // 记录每一行的宽度与高度
        int lineWidth = 0;
        int lineHeight = 0;
        
        // 得到内部元素的个数
        int cCount = getChildCount();
        
        for (int i = 0; i < cCount; i++)
        {
            View child = getChildAt(i);
            // 测量子View的宽和高
            measureChild(child, widthMeasureSpec, heightMeasureSpec);
            
            // 子View占据的宽度
            int childWidth = child.getMeasuredWidth();
            // 子View占据的高度
            int childHeight = child.getMeasuredHeight();
    
            LayoutParams lp = child.getLayoutParams();
            if (lp != null && lp instanceof MarginLayoutParams)
            {
                MarginLayoutParams mlp = (MarginLayoutParams) lp;
                // 子View占据的宽度
                childWidth += (mlp.leftMargin + mlp.rightMargin);
                // 子View占据的高度
                childHeight += (mlp.topMargin + mlp.bottomMargin);
            }
            
            // 换行
            if (lineWidth + childWidth > sizeWidth - getPaddingLeft()
                    - getPaddingRight())
            {
                // 对比得到最大的宽度
                width = Math.max(width, lineWidth);
                // 重置lineWidth
                lineWidth = childWidth;
                // 记录行高
                height += lineHeight;
                lineHeight = childHeight;
            }
            else
            // 未换行
            {
                // 叠加行宽
                lineWidth += childWidth;
                // 得到当前行最大的高度
                lineHeight = Math.max(lineHeight, childHeight);
            }
            // 最后一个控件
            if (i == cCount - 1)
            {
                width = Math.max(lineWidth, width);
                height += lineHeight;
            }
        }
        
        int measuredWidth = modeWidth == MeasureSpec.EXACTLY ? sizeWidth : width
                + getPaddingLeft() + getPaddingRight();
        int measuredHeight = modeHeight == MeasureSpec.EXACTLY ? sizeHeight : height
                + getPaddingTop() + getPaddingBottom();
        
        setMeasuredDimension(
                //
                measuredWidth,
                measuredHeight//
        );
        
    }
    
    /**
     * 设置子View的位置
     */
    @SuppressLint("DrawAllocation")
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b)
    {
        mAllViews.clear();
        mLineHeight.clear();
        
        // 当前ViewGroup的宽度
        int width = getWidth();
        
        int lineWidth = 0;
        int lineHeight = 0;
        
        List<View> lineViews = new ArrayList<View>();
        
        int cCount = getChildCount();
        
        for (int i = 0; i < cCount; i++)
        {
            View child = getChildAt(i);
            MarginLayoutParams lp = (MarginLayoutParams) child
                    .getLayoutParams();
            
            int childWidth = child.getMeasuredWidth();
            int childHeight = child.getMeasuredHeight();
            
            // 如果需要换行
            if (childWidth + lineWidth + lp.leftMargin + lp.rightMargin > width
                    - getPaddingLeft() - getPaddingRight())
            {
                // 记录LineHeight
                mLineHeight.add(lineHeight);
                // 记录当前行的Views
                mAllViews.add(lineViews);
                
                // 重置我们的行宽和行高
                lineWidth = 0;
                lineHeight = childHeight + lp.topMargin + lp.bottomMargin;
                // 重置我们的View集合
                lineViews = new ArrayList<View>();
            }
            lineWidth += childWidth + lp.leftMargin + lp.rightMargin;
            lineHeight = Math.max(lineHeight, childHeight + lp.topMargin
                    + lp.bottomMargin);
            lineViews.add(child);
            
        }// for end
        // 处理最后一行
        mLineHeight.add(lineHeight);
        mAllViews.add(lineViews);
        
        // 设置子View的位置
        
        int left = getPaddingLeft();
        int top = getPaddingTop();
        
        // 行数
        int lineNum = mAllViews.size();
        
        for (int i = 0; i < lineNum; i++)
        {
            // 当前行的所有的View
            lineViews = mAllViews.get(i);
            lineHeight = mLineHeight.get(i);
            
            for (int j = 0; j < lineViews.size(); j++)
            {
                View child = lineViews.get(j);
                // 判断child的状态
                if (child.getVisibility() == View.GONE)
                {
                    continue;
                }
                
                MarginLayoutParams lp = (MarginLayoutParams) child
                        .getLayoutParams();
                
                int lc = left + lp.leftMargin;
                int tc = top + lp.topMargin;
                //如果第一个不设置左Padding的话
                if (j == 0 && mIsFirstNoLeftPadding)
                {
                    lc = left;
                    
                }
                
                if (mIsFirstLineNoTopPadding)
                {
                    if (i == 0)
                    {
                        mFirstLineTopHeight = lp.topMargin;
                    }
                    tc = tc - mFirstLineTopHeight;
                }
                
                
                int rc = lc + child.getMeasuredWidth();
                int bc = tc + child.getMeasuredHeight();
                
                // 为子View进行布局
                child.layout(lc, tc, rc, bc);
                
                if (mIsFirstNoLeftPadding && j == 0)
                {
                    left += child.getMeasuredWidth()
                            + lp.rightMargin;
                }
                else
                {
                    left += child.getMeasuredWidth() + lp.leftMargin
                            + lp.rightMargin;
                }
            }
            left = getPaddingLeft();
            top += lineHeight;
        }
        
    }
    
    /**
     * 设置左边第一个View是否有左Padding
     *
     * @param noLeftPadding
     */
    public void setIsFirstNoLeftPadding(boolean noLeftPadding)
    {
        this.mIsFirstNoLeftPadding = noLeftPadding;
    }
    
    public void setIsFirstLineNoTopPadding(boolean noTopPadding)
    {
        this.mIsFirstLineNoTopPadding = noTopPadding;
    }
    
    /**
     * 与当前ViewGroup对应的LayoutParams
     */
    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs)
    {
        return new MarginLayoutParams(getContext(), attrs);
    }
    
}
