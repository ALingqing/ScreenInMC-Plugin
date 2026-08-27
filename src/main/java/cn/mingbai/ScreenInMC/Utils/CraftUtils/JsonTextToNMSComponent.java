package cn.mingbai.ScreenInMC.Utils.CraftUtils;

import cn.mingbai.ScreenInMC.Utils.LangUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

import static cn.mingbai.ScreenInMC.Utils.CraftUtils.CraftUtils.getMethod;

public class JsonTextToNMSComponent {
    static Class ChatBaseComponentClass;
    static Class ChatMessageClass;
    static Class ChatComponentKeybindClass;
    static Class ChatComponentTextClass;
    static Class ChatModifierClass;
    static Method ChatModifierSetBold;
    static Method ChatModifierSetItalic;
    static Method ChatModifierSetStrikethrough;
    static Method ChatModifierSetUnderline;
    static Method ChatModifierSetRandom;
    static Method ChatModifierSetColor;
    static Method ChatModifierSetChatClickable;
    static Method ChatModifierSetChatHoverable;
    static Class EnumChatFormatClass;
    static Class ChatClickableClass;
    static Class EnumClickActionClass;
    static Method ChatBaseComponentSetChatModifier;
    static Method ChatBaseComponentAddSibling;
    static Class ChatHexColorClass;
    static Method ChatHexColorFromEnumChatFormat;
    static Constructor ChatMessageClassConstructor;
    static Constructor ChatClickableClassConstructor;
    static Constructor LiteralContentsClassConstructor;
    static Constructor KeybindContentsClassConstructor;
    static Method KeybindContentsFactory;
    static Constructor TranslatableContentsClassConstructor;
    static Constructor ChatComponentKeybindClassConstructor;
    static Constructor ChatComponentTextClassConstructor;
    static Constructor ChatModifierClassConstructor;
    //26.x：Style 是接口时用 EMPTY + withXxx 构建
    static java.lang.reflect.Field StyleEmpty;
    static Method StyleEmptyMethod;
    static Method StyleWithColor;
    static Method StyleWithBold;
    static Method StyleWithItalic;
    static Method StyleWithUnderlined;
    static Method StyleWithStrikethrough;
    static Method StyleWithObfuscated;
    static Method StyleWithClickEvent;
    //1.19+
    static Class LiteralContentsClass;
    static Class KeybindContentsClass;
    static Class TranslatableContentsClass;
    static Class IChatMutableComponentClass;
    static Method IChatMutableComponentFromComponentContents;
    static Method LiteralContentsClassFactory;
    //26.x：ClickEvent 变为 sealed 接口，action 名 -> [构造器, 参数类型数组]
    static Map<String, Object[]> clickEventConstructors = new HashMap<>();

    public static void init() throws Exception {
        ChatModifierClass = CraftUtils.getMinecraftClass("ChatModifier");
        if(ChatModifierClass==null){
            ChatModifierClass = CraftUtils.getMinecraftClass("Style");
        }
        ChatModifierClassConstructor = CraftUtils.getConstructor(ChatModifierClass);
        if(ChatModifierClassConstructor!=null) {
            ChatModifierClassConstructor.setAccessible(true);
        }else if(ChatModifierClass!=null && ChatModifierClass.isInterface()){
            // 26.x：Style 是接口，不可 new。用 Style.EMPTY + withXxx 链式构建。
            // 收集静态 EMPTY 字段/方法和 with 方法
            try {
                StyleEmpty = ChatModifierClass.getField("EMPTY");
            }catch (NoSuchFieldException e){
                for(Method m : ChatModifierClass.getDeclaredMethods()){
                    if(Modifier.isStatic(m.getModifiers())&&m.getParameterCount()==0&&m.getName().equals("empty")){
                        StyleEmptyMethod = m;
                        break;
                    }
                }
            }
            for(Method m : ChatModifierClass.getDeclaredMethods()){
                String n = m.getName();
                if(m.getParameterCount()==1){
                    String pn = m.getParameters()[0].getType().getSimpleName();
                    if(n.equals("withColor")||pn.equals("TextColor")&&n.startsWith("with")) StyleWithColor = m;
                    if(n.equals("withBold")||(pn.equals("Boolean")&&n.equals("withBold"))) StyleWithBold = m;
                    if(n.equals("withItalic")||(pn.equals("Boolean")&&n.equals("withItalic"))) StyleWithItalic = m;
                    if(n.equals("withUnderlined")||(pn.equals("Boolean")&&n.equals("withUnderlined"))) StyleWithUnderlined = m;
                    if(n.equals("withStrikethrough")||(pn.equals("Boolean")&&n.equals("withStrikethrough"))) StyleWithStrikethrough = m;
                    if(n.equals("withObfuscated")||(pn.equals("Boolean")&&n.equals("withObfuscated"))) StyleWithObfuscated = m;
                    if(n.equals("withClickEvent")||(pn.equals("ClickEvent")&&n.startsWith("with"))) StyleWithClickEvent = m;
                }
            }
        }
        EnumChatFormatClass = CraftUtils.getMinecraftClass("EnumChatFormat");
        if(EnumChatFormatClass==null){
            EnumChatFormatClass = CraftUtils.getMinecraftClass("ChatFormatting");
        }
        ChatClickableClass = CraftUtils.getMinecraftClass("ChatClickable");
        if(ChatClickableClass == null){
            ChatClickableClass = CraftUtils.getMinecraftClass("ClickEvent");
        }
        ChatClickableClassConstructor = CraftUtils.getConstructor(ChatClickableClass);
        if(ChatClickableClassConstructor==null && ChatClickableClass!=null && ChatClickableClass.isInterface()){
            // 26.x：ClickEvent 是 sealed 接口，子类是静态嵌套类（OpenUrl/RunCommand/SuggestCommand/CopyToClipboard/ChangePage 等）
            // 收集各子类构造器，按 action 名称匹配
            for(Class i : ChatClickableClass.getDeclaredClasses()){
                if(i.isEnum() || i.isInterface()) continue;
                for(Constructor j : i.getDeclaredConstructors()){
                    if(j.getParameterCount()>=1){
                        String actionKey = i.getSimpleName().toLowerCase().replace("_","");
                        clickEventConstructors.put(actionKey, new Object[]{j, j.getParameterTypes()});
                    }
                }
            }
        }
        EnumClickActionClass = CraftUtils.getMinecraftClass("EnumClickAction");
        if(EnumClickActionClass==null){
            for(Class i : CraftUtils.getMinecraftClasses("Action",true)){
                if(i.getName().contains("ClickEvent")){
                    EnumClickActionClass = i;
                }
            }
        }
        if(CraftUtils.minecraftVersion>=19){
            LiteralContentsClass = CraftUtils.getMinecraftClass("LiteralContents");
            if(LiteralContentsClass==null){
                LiteralContentsClass = CraftUtils.getMinecraftClass("PlainTextContents");
            }
            KeybindContentsClass = CraftUtils.getMinecraftClass("KeybindContents");
            TranslatableContentsClass = CraftUtils.getMinecraftClass("TranslatableContents");
            IChatMutableComponentClass = CraftUtils.getMinecraftClass("IChatMutableComponent");
            if(IChatMutableComponentClass == null){
                IChatMutableComponentClass = CraftUtils.getMinecraftClass("MutableComponent");
            }
            try {
                LiteralContentsClassConstructor = CraftUtils.getConstructor(LiteralContentsClass);
            }catch (Exception e){
                LiteralContentsClassConstructor=null;
                literal:
                for(Class i:LiteralContentsClass.getDeclaredClasses()){
                    for(Constructor j:i.getDeclaredConstructors())
                    {
                        if(j.getParameterCount()==1&&j.getParameters()[0].getType().equals(String.class)){
                            LiteralContentsClassConstructor = j;
                            LiteralContentsClass = i;
                            break literal;
                        }
                    }
                }
                if(LiteralContentsClassConstructor==null) {
                    // 26.x 兼容：LiteralContents 可能是 PlainTextContents 的嵌套 record，
                    // 也可能只有静态工厂 PlainTextContents.create(String)。
                    // 兜底 1：PlainTextContents.create(String) 静态工厂
                    for(Method i:LiteralContentsClass.getDeclaredMethods()){
                        if(Modifier.isStatic(i.getModifiers())&&i.getParameterCount()==1&&i.getParameters()[0].getType().equals(String.class)&&i.getReturnType().equals(LiteralContentsClass)){
                            LiteralContentsClassFactory = i;
                            LiteralContentsClassFactory.setAccessible(true);
                            break;
                        }
                    }
                    if(LiteralContentsClassFactory==null){
                        // 兜底 2：遍历嵌套类找 create/literal/of 静态工厂
                        literal2:
                        for(Class i:LiteralContentsClass.getDeclaredClasses()){
                            for(Method j:i.getDeclaredMethods()){
                                if(Modifier.isStatic(j.getModifiers())&&j.getParameterCount()==1&&j.getParameters()[0].getType().equals(String.class)&&j.getReturnType().equals(i)){
                                    LiteralContentsClassFactory = j;
                                    LiteralContentsClassFactory.setAccessible(true);
                                    LiteralContentsClass = i;
                                    break literal2;
                                }
                            }
                        }
                    }
                    if(LiteralContentsClassFactory==null) {
                        throw new RuntimeException("class LiteralContents not found.");
                    }
                }
            }
            KeybindContentsClassConstructor = CraftUtils.getConstructor(KeybindContentsClass);
            if(KeybindContentsClassConstructor==null && KeybindContentsClass!=null && KeybindContentsClass.isInterface()){
                // 26.x：KeybindContents 若是接口，找带 String 参数的静态工厂或嵌套实现类
                for(Method m : KeybindContentsClass.getDeclaredMethods()){
                    if(Modifier.isStatic(m.getModifiers())&&m.getParameterCount()==1&&m.getParameters()[0].getType().equals(String.class)){
                        KeybindContentsFactory = m;
                        KeybindContentsFactory.setAccessible(true);
                        break;
                    }
                }
                if(KeybindContentsFactory==null){
                    for(Class i : KeybindContentsClass.getDeclaredClasses()){
                        for(Constructor j : i.getDeclaredConstructors()){
                            if(j.getParameterCount()==1&&j.getParameters()[0].getType().equals(String.class)){
                                KeybindContentsClassConstructor = j;
                                KeybindContentsClass = i;
                                break;
                            }
                        }
                        if(KeybindContentsClassConstructor!=null) break;
                    }
                }
            }
            TranslatableContentsClassConstructor = CraftUtils.getConstructor(TranslatableContentsClass);
            if(TranslatableContentsClassConstructor==null && TranslatableContentsClass!=null && TranslatableContentsClass.isInterface()){
                for(Class i : TranslatableContentsClass.getDeclaredClasses()){
                    for(Constructor j : i.getDeclaredConstructors()){
                        if(j.getParameterCount()==1&&j.getParameters()[0].getType().equals(String.class)){
                            TranslatableContentsClassConstructor = j;
                            TranslatableContentsClass = i;
                            break;
                        }
                    }
                    if(TranslatableContentsClassConstructor!=null) break;
                }
            }
            ChatHexColorClass = CraftUtils.getMinecraftClass("ChatHexColor");
            if(ChatHexColorClass==null){
                ChatHexColorClass = CraftUtils.getMinecraftClass("TextColor");
            }
            for(Method i:IChatMutableComponentClass==null?new Method[0]:IChatMutableComponentClass.getDeclaredMethods()){
                if(i.getParameterCount()==1&&i.getParameters()[0].getType().getSimpleName().equals("ComponentContents")){
                    IChatMutableComponentFromComponentContents=i;
                }
            }
            if(IChatMutableComponentFromComponentContents==null){
                // 兜底：在 Component 类中查找 static create(ComponentContents)
                Class componentClass = CraftUtils.getMinecraftClass("Component");
                if(componentClass==null){
                    componentClass = CraftUtils.getMinecraftClass("IChatBaseComponent");
                }
                if(componentClass!=null){
                    for(Method i:componentClass.getDeclaredMethods()){
                        if(Modifier.isStatic(i.getModifiers())&&i.getParameterCount()==1&&i.getParameters()[0].getType().getSimpleName().equals("ComponentContents")){
                            IChatMutableComponentFromComponentContents=i;
                            break;
                        }
                    }
                }
            }
            if(IChatMutableComponentFromComponentContents==null) throw new RuntimeException("public static IChatMutableComponent ...(ComponentContents ...) not found.");
        }else {
            ChatBaseComponentClass = CraftUtils.getMinecraftClass("ChatBaseComponent");
            ChatComponentTextClass = CraftUtils.getMinecraftClass("ChatComponentText");
            ChatComponentTextClassConstructor = CraftUtils.getConstructor(ChatComponentTextClass);
            ChatMessageClass = CraftUtils.getMinecraftClass("ChatMessage");
            ChatMessageClassConstructor = CraftUtils.getConstructor(ChatMessageClass);

            if (CraftUtils.minecraftVersion >= 16) {
                ChatComponentKeybindClass = CraftUtils.getMinecraftClass("ChatComponentKeybind");
                ChatComponentKeybindClassConstructor = CraftUtils.getConstructor(ChatComponentKeybindClass);
                ChatHexColorClass = CraftUtils.getMinecraftClass("ChatHexColor");
                if(ChatHexColorClass==null){
                    ChatHexColorClass = CraftUtils.getMinecraftClass("TextColor");
                }
            } else {
                if (CraftUtils.minecraftVersion >= 12) {
                    ChatComponentKeybindClass = CraftUtils.getMinecraftClass("ChatComponentKeybind");
                    ChatComponentKeybindClassConstructor = CraftUtils.getConstructor(ChatComponentKeybindClass);
                }
                ChatModifierSetBold = getMethod(ChatModifierClass,"setBold");
                ChatModifierSetItalic = getMethod(ChatModifierClass,"setItalic");
                ChatModifierSetStrikethrough = getMethod(ChatModifierClass,"setStrikethrough");
                ChatModifierSetUnderline = getMethod(ChatModifierClass,"setUnderline");
                ChatModifierSetRandom = getMethod(ChatModifierClass,"setRandom");
                ChatModifierSetColor = getMethod(ChatModifierClass,"setColor");
                ChatModifierSetChatClickable = getMethod(ChatModifierClass,"setChatClickable");
                ChatModifierSetChatHoverable = getMethod(ChatModifierClass,"setChatHoverable");
            }
        }
        if(ChatHexColorClass!=null) {
            for (Method i : ChatHexColorClass.getDeclaredMethods()) {
                if (i.getParameterCount() == 1 && i.getParameters()[0].getType().equals(EnumChatFormatClass)) {
                    ChatHexColorFromEnumChatFormat = i;
                    ChatHexColorFromEnumChatFormat.setAccessible(true);
                }
            }
            // 26.x：不强制要求该转换方法存在，getColor 会兜底返回 null
        }
        Class modifierClass = IChatMutableComponentClass!=null?IChatMutableComponentClass:ChatBaseComponentClass;
        if(modifierClass!=null){
        for(Method i:modifierClass.getDeclaredMethods()){
            if(i.getParameterCount()==1){
                String paramName = i.getParameters()[0].getType().getSimpleName();
                if(paramName.equals("ChatModifier") || paramName.equals("Style")) {
                    ChatBaseComponentSetChatModifier = i;
                }
                if(paramName.equals("IChatBaseComponent") || paramName.equals("Component")){
                    ChatBaseComponentAddSibling = i;
                }
            }
        }
        }
        // 不再强制 throw，缺失的方法在运行时判空兜底
    }
    public static String getKeybind(String keybind){
        switch (keybind){
            case "key.sneak":
                return "Shift(Default)";
            case "key.attack":
                return "Left-Click(Default)";
            case "key.use":
                return "Right-Click(Default)";
        }
        return "Unknown";
    }
    private static Object getColor(String color) throws Exception{
        if(color==null) return null;
        Object o = Enum.valueOf(EnumChatFormatClass, color.toUpperCase());
        if (ChatHexColorClass != null && ChatHexColorFromEnumChatFormat != null) {
            o = ChatHexColorFromEnumChatFormat.invoke(null, o);
        }
        return o;
    }
    private static Object getClickEvent(LangUtils.JsonText.ClickEvent event) throws Exception {
        if(event==null) return null;
        // 26.x：ClickEvent 是 sealed 接口，按 action 匹配子类构造器
        if(ChatClickableClassConstructor==null && !clickEventConstructors.isEmpty()){
            String actionKey = (event.action==null?"":event.action).toLowerCase().replace("_","").replace(" ","");
            Object[] entry = clickEventConstructors.get(actionKey);
            if(entry==null){
                // 常见别名映射：run_command/runcommand 等
                String alias = null;
                if(actionKey.contains("command")) alias = "runcommand";
                else if(actionKey.contains("url")||actionKey.contains("link")) alias = "openurl";
                else if(actionKey.contains("copy")) alias = "copytoclipboard";
                else if(actionKey.contains("page")) alias = "changepage";
                else if(actionKey.contains("suggest")) alias = "suggestcommand";
                if(alias!=null) entry = clickEventConstructors.get(alias);
            }
            if(entry!=null){
                Constructor c = (Constructor) entry[0];
                Class[] paramTypes = (Class[]) entry[1];
                if(paramTypes.length==1 && paramTypes[0].equals(String.class)){
                    return c.newInstance(event.value);
                }else if(paramTypes.length==1 && paramTypes[0].equals(int.class)){
                    try { return c.newInstance(Integer.parseInt(event.value)); }catch (Exception ignored){ return null; }
                }else if(paramTypes.length==2 && paramTypes[0].equals(String.class) && paramTypes[1].equals(String.class)){
                    return c.newInstance(event.action, event.value);
                }
            }
            // 找不到匹配时安全返回 null（不抛异常）
            return null;
        }
        Object action = Enum.valueOf(EnumClickActionClass, event.action.toUpperCase());
        Object chatClickable = ChatClickableClassConstructor.newInstance(action, event.value);
        return chatClickable;
    }
    public static Object jsonTextToComponent(LangUtils.JsonText text){
        try {
            Object obj=null;
            if(text.translate!=null){
                if(CraftUtils.minecraftVersion>=19) {
                    for(Constructor i : TranslatableContentsClass.getDeclaredConstructors()){
                        if(i.getParameterCount()==1){
                            obj = i.newInstance(text.translate);
                            break;
                        }
                        if(i.getParameterCount()==3){
                            obj = i.newInstance(text.translate,null,new Object[0]);
                        }
                    }
                    if(obj==null){
                        // 找不到合适构造器时兜底为纯文本
                        if(LiteralContentsClassConstructor!=null){
                            obj = LiteralContentsClassConstructor.newInstance(text.translate);
                        }else{
                            obj = LiteralContentsClassFactory.invoke(null,text.translate);
                        }
                    }
                }else
                if(ChatMessageClass!=null){
                    obj = ChatMessageClassConstructor.newInstance(text.translate);
                }else{
                    throw new RuntimeException("ChatMessageClass not found.");
                }
            }else if(text.keybind!=null){
                if(CraftUtils.minecraftVersion>=19) {
                    if(KeybindContentsClassConstructor!=null){
                        obj = KeybindContentsClassConstructor.newInstance(text.keybind);
                    }else if(KeybindContentsFactory!=null){
                        obj = KeybindContentsFactory.invoke(null,text.keybind);
                    }else{
                        // 兜底：当作纯文本
                        if(LiteralContentsClassConstructor!=null){
                            obj = LiteralContentsClassConstructor.newInstance(text.keybind);
                        }else{
                            obj = LiteralContentsClassFactory.invoke(null,text.keybind);
                        }
                    }
                }else
                if(ChatComponentKeybindClass!=null){
                    obj = ChatComponentKeybindClassConstructor.newInstance(text.keybind);
                }else{
                    obj = ChatComponentTextClassConstructor.newInstance(getKeybind(text.keybind));
                }
            }else {
                if(CraftUtils.minecraftVersion>=19) {
                    if(LiteralContentsClassConstructor!=null){
                        obj = LiteralContentsClassConstructor.newInstance(text.text == null ? "" : text.text);
                    }else{
                        obj = LiteralContentsClassFactory.invoke(null,text.text == null ? "" : text.text);
                    }
                }else {
                    obj = ChatComponentTextClassConstructor.newInstance(text.text == null ? "" : text.text);
                }
            }
            if(CraftUtils.minecraftVersion>=19 && IChatMutableComponentFromComponentContents!=null) {
                obj=IChatMutableComponentFromComponentContents.invoke(null,obj);
            }
            Object chatModifier = null;
            if(ChatModifierClassConstructor==null && ChatModifierClass!=null && ChatModifierClass.isInterface()){
                // 26.x：Style 是接口，从 EMPTY 开始链式 withXxx 构建
                Object style = null;
                if(StyleEmpty!=null){
                    style = StyleEmpty.get(null);
                }else if(StyleEmptyMethod!=null){
                    style = StyleEmptyMethod.invoke(null);
                }
                if(style!=null){
                    if(text.color!=null && StyleWithColor!=null){
                        style = StyleWithColor.invoke(style, getColor(text.color));
                    }
                    if(text.bold!=null && StyleWithBold!=null){
                        style = StyleWithBold.invoke(style, text.bold);
                    }
                    if(text.italic!=null && StyleWithItalic!=null){
                        style = StyleWithItalic.invoke(style, text.italic);
                    }
                    if(text.underlined!=null && StyleWithUnderlined!=null){
                        style = StyleWithUnderlined.invoke(style, text.underlined);
                    }
                    if(text.strikethrough!=null && StyleWithStrikethrough!=null){
                        style = StyleWithStrikethrough.invoke(style, text.strikethrough);
                    }
                    if(text.obfuscated!=null && StyleWithObfuscated!=null){
                        style = StyleWithObfuscated.invoke(style, text.obfuscated);
                    }
                    if(text.clickEvent!=null && StyleWithClickEvent!=null){
                        Object clickEvent = getClickEvent(text.clickEvent);
                        if(clickEvent!=null) style = StyleWithClickEvent.invoke(style, clickEvent);
                    }
                    chatModifier = style;
                }
            }
            else if(CraftUtils.minecraftVersion>=16){
                if(ChatModifierClassConstructor.getParameterCount()==10) {
                    chatModifier = ChatModifierClassConstructor.newInstance(
                            getColor(text.color),
                            text.bold,
                            text.italic,
                            text.underlined,
                            text.strikethrough,
                            text.obfuscated,
                            getClickEvent(text.clickEvent),
                            null,
                            null,
                            null
                    );
                }else{
                    chatModifier = ChatModifierClassConstructor.newInstance(
                            getColor(text.color),
                            text.bold,
                            text.italic,
                            text.underlined,
                            text.strikethrough,
                            text.obfuscated,
                            getClickEvent(text.clickEvent),
                            null,
                            null,
                            null,
                            null
                    );
                }

            }else {
                chatModifier = ChatModifierClassConstructor.newInstance();
                if (text.bold != null) {
                    ChatModifierSetBold.invoke(chatModifier, text.bold);
                }
                if (text.italic != null) {
                    ChatModifierSetItalic.invoke(chatModifier, text.italic);
                }
                if (text.strikethrough != null) {
                    ChatModifierSetStrikethrough.invoke(chatModifier, text.strikethrough);
                }
                if (text.underlined != null) {
                    ChatModifierSetUnderline.invoke(chatModifier, text.underlined);
                }
                if (text.obfuscated != null) {
                    ChatModifierSetRandom.invoke(chatModifier, text.obfuscated);
                }
                if (text.bold != null) {
                    ChatModifierSetBold.invoke(chatModifier, text.bold);
                }
                if (text.color != null) {
                    ChatModifierSetColor.invoke(chatModifier, getColor(text.color));
                }
                if (text.clickEvent != null) {
                    ChatModifierSetChatClickable.invoke(chatModifier, getClickEvent(text.clickEvent));
                }
            }
            if(chatModifier!=null && ChatBaseComponentSetChatModifier!=null) {
                ChatBaseComponentSetChatModifier.invoke(obj, chatModifier);
            }
            if(text.extra!=null && ChatBaseComponentAddSibling!=null){
                ChatBaseComponentAddSibling.invoke(obj,jsonTextToComponent(text.extra));
            }
            return obj;

        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }
}
