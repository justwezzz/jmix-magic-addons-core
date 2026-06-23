package org.magic.jmix.addons.core.entity;

/**
 * 用户菜单标题提供者。
 * <p>
 * User 实体可实现此接口以提供自定义的用户菜单显示标题。
 * <p>
 * 示例：
 * <pre>
 * &#64;Entity(name = "User")
 * public class User implements UserDetails, UserMenuTitleProvider {
 *     ...
 *     &#64;Override
 *     public String getMenuTitle() {
 *         return getFirstName() + " " + getLastName();
 *     }
 * }
 * </pre>
 */
public interface UserMenuTitleProvider {

    /**
     * 获取用户菜单显示标题。
     *
     * @return 显示在用户菜单中的标题
     */
    String getMenuTitle();
}
