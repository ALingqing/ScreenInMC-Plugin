package cn.mingbai.ScreenInMC.Utils.CraftUtils;

public interface OutPacket {
    static void initAll() throws Exception{
        // 每个子模块独立 try-catch：一个失败不阻断其他模块初始化
        try { OutMapPacket.init(); }catch (Throwable t){ CraftUtils.LOGGER.warning("ScreenInMC OutMapPacket.init() failed: "+t); }
        try { OutSetMapEntityPacket.init(); }catch (Throwable t){ CraftUtils.LOGGER.warning("ScreenInMC OutSetMapEntityPacket.init() failed: "+t); }
        try { OutAddMapEntityPacket.init(); }catch (Throwable t){ CraftUtils.LOGGER.warning("ScreenInMC OutAddMapEntityPacket.init() failed: "+t); }
        try { OutActionBarPacket.init(); }catch (Throwable t){ CraftUtils.LOGGER.warning("ScreenInMC OutActionBarPacket.init() failed: "+t); }
        try { OutSetSlotPacket.init(); }catch (Throwable t){ CraftUtils.LOGGER.warning("ScreenInMC OutSetSlotPacket.init() failed: "+t); }
        try { OutWindowDataPacket.init(); }catch (Throwable t){ CraftUtils.LOGGER.warning("ScreenInMC OutWindowDataPacket.init() failed: "+t); }
        try { OutOpenWindowPacket.init(); }catch (Throwable t){ CraftUtils.LOGGER.warning("ScreenInMC OutOpenWindowPacket.init() failed: "+t); }
        try { OutOpenBookPacket.init(); }catch (Throwable t){ CraftUtils.LOGGER.warning("ScreenInMC OutOpenBookPacket.init() failed: "+t); }
        try { OutWindowItemsPacket.init(); }catch (Throwable t){ CraftUtils.LOGGER.warning("ScreenInMC OutWindowItemsPacket.init() failed: "+t); }
        try { OutSystemMessagePacket.init(); }catch (Throwable t){ CraftUtils.LOGGER.warning("ScreenInMC OutSystemMessagePacket.init() failed: "+t); }
        try { OutRemoveMapEntityPacket.init(); }catch (Throwable t){ CraftUtils.LOGGER.warning("ScreenInMC OutRemoveMapEntityPacket.init() failed: "+t); }
        try { OutCloseWindowPacket.init(); }catch (Throwable t){ CraftUtils.LOGGER.warning("ScreenInMC OutCloseWindowPacket.init() failed: "+t); }
    }
}
