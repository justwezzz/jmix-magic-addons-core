package org.magic.jmix.addons.core.component;

import java.util.Collections;
import java.util.List;

/**
 * 游标分页的一页结果。
 * <p>
 * 用于描述一次游标分页加载返回的数据：当前页项目、下一页游标以及是否仍有更多数据。
 *
 * @param <T> 数据项类型
 */
public class CursorPage<T> {

    private final List<T> items;
    private final String nextCursor;
    private final boolean hasMore;

    /**
     * 创建一页游标分页结果。
     *
     * @param items      当前页数据，可为 null（按空列表处理）
     * @param nextCursor 下一页游标，可为 null
     * @param hasMore    是否仍有更多数据
     */
    public CursorPage(List<T> items, String nextCursor, boolean hasMore) {
        this.items = items != null ? items : Collections.emptyList();
        this.nextCursor = nextCursor;
        this.hasMore = hasMore;
    }

    /**
     * 创建一页游标分页结果。
     *
     * @param items      当前页数据，可为 null（按空列表处理）
     * @param nextCursor 下一页游标，可为 null
     * @param hasMore    是否仍有更多数据
     * @param <T>        数据项类型
     * @return 游标分页结果
     */
    public static <T> CursorPage<T> of(List<T> items, String nextCursor, boolean hasMore) {
        return new CursorPage<>(items, nextCursor, hasMore);
    }

    /**
     * @return 当前页数据
     */
    public List<T> getItems() {
        return items;
    }

    /**
     * @return 下一页游标，可为 null
     */
    public String getNextCursor() {
        return nextCursor;
    }

    /**
     * @return 是否仍有更多数据
     */
    public boolean isHasMore() {
        return hasMore;
    }
}
