package cn.mingbai.ScreenInMC.Utils.CraftUtils;

public interface InPacket {
    Class getNMSClass();
    static void initAll() throws Exception {
        // 每个子模块独立 try-catch：一个失败不阻断其他
        try { InWindowClosePacket.init(); }catch (Throwable t){ CraftUtils.LOGGER.warning("ScreenInMC InWindowClosePacket.init() failed: "+t); }
        try { InWindowClickPacket.init(); }catch (Throwable t){ CraftUtils.LOGGER.warning("ScreenInMC InWindowClickPacket.init() failed: "+t); }
        try { InAnvilRenamePacket.init(); }catch (Throwable t){ CraftUtils.LOGGER.warning("ScreenInMC InAnvilRenamePacket.init() failed: "+t); }
        try { InClickEntityPacket.init(); }catch (Throwable t){ CraftUtils.LOGGER.warning("ScreenInMC InClickEntityPacket.init() failed: "+t); }
    }
    boolean load(Object obj);
    static InPacket create(Object obj){
        InPacket inPacket;
        inPacket = new InWindowClosePacket();
        if(obj.getClass().equals(inPacket.getNMSClass())) if(inPacket.load(obj)) return inPacket;

        inPacket = new InWindowClickPacket();
        if(obj.getClass().equals(inPacket.getNMSClass())) if(inPacket.load(obj)) return inPacket;

        inPacket = new InAnvilRenamePacket();
        if(obj.getClass().equals(inPacket.getNMSClass())) if(inPacket.load(obj)) return inPacket;

        inPacket = new InClickEntityPacket();
        if(obj.getClass().equals(inPacket.getNMSClass())) if(inPacket.load(obj)) return inPacket;

        return null;
    }
}
