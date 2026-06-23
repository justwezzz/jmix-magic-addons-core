package org.magic.jmix.addons.core.component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记可在 Kaoto 可视化编辑器的 ref 下拉框中展示的 Spring Bean。
 * 只有带此注解的 Bean 才会出现在 process.ref、bean.type 等字段的自动补全列表中。
 *
 * <p>用法：
 * <pre>
 * {@code @KaotoBean(description = "文件上传处理器")}
 * {@code @Component}
 * public class FileUploadProcessor implements Processor { ... }
 * </pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface KaotoBean {

    /**
     * Bean 的描述信息，显示在 Kaoto 下拉框的 description 列。
     */
    String description() default "";
}
