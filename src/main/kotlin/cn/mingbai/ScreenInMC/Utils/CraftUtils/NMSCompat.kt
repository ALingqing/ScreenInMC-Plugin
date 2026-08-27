package cn.mingbai.ScreenInMC.Utils.CraftUtils

import cn.mingbai.ScreenInMC.Utils.LangUtils
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.util.Locale

/**
 * 26.2 (Leaf/Paper 年份版本号) 适配辅助工具。
 *
 * 26.2 对 chat 组件做了大规模重构：
 *  - Style 不再一定能 new（可能是接口，也可能只有特定构造器）
 *  - ClickEvent 变成 sealed 接口，子类为静态嵌套类
 *  - 很多类变成接口导致 getDeclaredConstructors() 为空数组
 *
 * 全部逻辑用 Kotlin 编写，Java 侧调用。
 */
object NMSCompat {

    /**
     * 安全获取构造器：接口/无构造器类返回 null，而不是抛 ArrayIndexOutOfBoundsException。
     */
    @JvmStatic
    fun safeGetConstructor(cls: Class<*>?): Constructor<*>? {
        if (cls == null) return null
        return try {
            cls.getDeclaredConstructor()
        } catch (e: NoSuchMethodException) {
            val constructors = cls.declaredConstructors
            if (constructors.isEmpty()) null else constructors[0]
        } catch (e: Exception) {
            val constructors = cls.declaredConstructors
            if (constructors.isEmpty()) null else constructors[0]
        }
    }

    /**
     * 收集 Style 的 EMPTY 静态字段/方法 和 withXxx 方法。
     * 返回一个 StyleBuilder 描述对象。
     */
    @JvmStatic
    fun collectStyleBuilder(styleClass: Class<*>?): StyleBuilder? {
        if (styleClass == null) return null
        val builder = StyleBuilder()
        builder.styleClass = styleClass
        // EMPTY 静态字段
        builder.emptyField = try {
            styleClass.getField("EMPTY")
        } catch (e: NoSuchFieldException) {
            null
        }
        // 或 empty() 静态方法
        if (builder.emptyField == null) {
            for (m in styleClass.declaredMethods) {
                if (Modifier.isStatic(m.modifiers) && m.parameterCount == 0 && m.name == "empty") {
                    builder.emptyMethod = m
                    break
                }
            }
        }
        // withXxx 方法
        for (m in styleClass.declaredMethods) {
            if (m.parameterCount != 1) continue
            val n = m.name
            val pn = m.parameters[0].type.simpleName
            when {
                n == "withColor" || (pn == "TextColor" && n.startsWith("with")) -> builder.withColor = m
                n == "withBold" -> builder.withBold = m
                n == "withItalic" -> builder.withItalic = m
                n == "withUnderlined" -> builder.withUnderlined = m
                n == "withStrikethrough" -> builder.withStrikethrough = m
                n == "withObfuscated" -> builder.withObfuscated = m
                n == "withClickEvent" -> builder.withClickEvent = m
            }
        }
        return builder
    }

    /**
     * 从 EMPTY 开始链式构建 Style 对象。
     * @return 构建好的 Style 对象；若无法构建返回 null
     */
    @JvmStatic
    fun buildStyle(
        builder: StyleBuilder?,
        color: Any?,
        bold: Boolean?,
        italic: Boolean?,
        underlined: Boolean?,
        strikethrough: Boolean?,
        obfuscated: Boolean?,
        clickEvent: Any?
    ): Any? {
        if (builder == null) return null
        val emptyField = builder.emptyField
        val emptyMethod = builder.emptyMethod
        val withColor = builder.withColor
        val withBold = builder.withBold
        val withItalic = builder.withItalic
        val withUnderlined = builder.withUnderlined
        val withStrikethrough = builder.withStrikethrough
        val withObfuscated = builder.withObfuscated
        val withClickEvent = builder.withClickEvent
        var style: Any? = null
        if (emptyField != null) style = emptyField.get(null)
        if (style == null && emptyMethod != null) {
            style = emptyMethod.invoke(null)
        }
        if (style == null) return null
        if (color != null && withColor != null) style = withColor.invoke(style, color)
        if (bold != null && withBold != null) style = withBold.invoke(style, bold)
        if (italic != null && withItalic != null) style = withItalic.invoke(style, italic)
        if (underlined != null && withUnderlined != null) style = withUnderlined.invoke(style, underlined)
        if (strikethrough != null && withStrikethrough != null) style = withStrikethrough.invoke(style, strikethrough)
        if (obfuscated != null && withObfuscated != null) style = withObfuscated.invoke(style, obfuscated)
        if (clickEvent != null && withClickEvent != null) style = withClickEvent.invoke(style, clickEvent)
        return style
    }

    /**
     * 26.x：ClickEvent 是 sealed 接口，子类是静态嵌套类（OpenUrl/RunCommand/SuggestCommand/CopyToClipboard/ChangePage 等）。
     * 收集各子类构造器，按 action 名称（小写、去下划线）匹配。
     * @return Map<actionKey, Array<Any>> = Map<actionKey, [Constructor, paramTypes]>
     */
    @JvmStatic
    fun collectClickEventConstructors(clickEventClass: Class<*>?): MutableMap<String, Array<Any>> {
        val map = HashMap<String, Array<Any>>()
        if (clickEventClass == null) return map
        for (sub in clickEventClass.declaredClasses) {
            if (sub.isEnum || sub.isInterface) continue
            for (ctor in sub.declaredConstructors) {
                if (ctor.parameterCount >= 1) {
                    val actionKey = sub.simpleName.lowercase(Locale.ROOT).replace("_", "")
                    map[actionKey] = arrayOf(ctor as Any, ctor.parameterTypes as Any)
                }
            }
        }
        return map
    }

    /**
     * 根据 action 字符串创建 ClickEvent 对象。
     */
    @JvmStatic
    fun createClickEvent(
        map: Map<String, Array<Any>>,
        action: String?,
        value: String?
    ): Any? {
        if (map.isEmpty()) return null
        val actionKey = (action ?: "").lowercase(Locale.ROOT).replace("_", "").replace(" ", "")
        var entry = map[actionKey]
        if (entry == null) {
            val alias = when {
                actionKey.contains("command") -> "runcommand"
                actionKey.contains("url") || actionKey.contains("link") -> "openurl"
                actionKey.contains("copy") -> "copytoclipboard"
                actionKey.contains("page") -> "changepage"
                actionKey.contains("suggest") -> "suggestcommand"
                else -> null
            }
            if (alias != null) entry = map[alias]
        }
        if (entry == null) return null
        val ctor = entry[0] as Constructor<*>
        val paramTypes = entry[1] as Array<*>
        return try {
            when {
                paramTypes.size == 1 && paramTypes[0] == String::class.java -> ctor.newInstance(value)
                paramTypes.size == 1 && paramTypes[0] == Int::class.javaPrimitiveType -> ctor.newInstance(value?.toIntOrNull() ?: return null)
                paramTypes.size == 2 && paramTypes[0] == String::class.java && paramTypes[1] == String::class.java -> ctor.newInstance(action, value)
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 26.x：unbreakable 组件是 NonValued（存在即 true），值为 Unit.INSTANCE 单例。
     */
    @JvmStatic
    fun getUnitInstance(): Any? {
        return try {
            val cls = Class.forName("net.minecraft.util.Unit")
            val f = cls.getField("INSTANCE")
            f.setAccessible(true)
            f.get(null)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 26.x：CustomModelData 是 (FloatList floats, BooleanList flags, List<String> strings, IntList colors)。
     * 旧版 setCustomModelData(int) 等价于 floats=[(float)data]。
     */
    @JvmStatic
    fun newCustomModelData(ctor: Constructor<*>, data: Int): Any? {
        return try {
            if (ctor.parameterCount == 1) {
                ctor.newInstance(data)
            } else {
                val floats = ArrayList<Float>()
                floats.add(data.toFloat())
                ctor.newInstance(floats, ArrayList<Any>(), ArrayList<Any>(), ArrayList<Any>())
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Style 链式构建器描述。
     */
    class StyleBuilder {
        var styleClass: Class<*>? = null
        var emptyField: Field? = null
        var emptyMethod: Method? = null
        var withColor: Method? = null
        var withBold: Method? = null
        var withItalic: Method? = null
        var withUnderlined: Method? = null
        var withStrikethrough: Method? = null
        var withObfuscated: Method? = null
        var withClickEvent: Method? = null
    }
}
