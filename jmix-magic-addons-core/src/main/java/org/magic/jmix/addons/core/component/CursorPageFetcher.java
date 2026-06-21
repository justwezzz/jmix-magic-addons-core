package org.magic.jmix.addons.core.component;

import javax.annotation.Nullable;

/**
 * 游标分页加载函数。
 * <p>
 * 调用方根据传入游标返回下一页数据。cursor 为 null 表示从头开始加载。
 *
 * @param <T> 数据项类型
 */
@FunctionalInterface
public interface CursorPageFetcher<T> {

    /**
     * 按游标加载下一页数据。
     *
     * @param cursor 当前游标，null 表示从头加载
     * @return 当前页游标分页结果
     */
    CursorPage<T> fetch(@Nullable String cursor);
}
