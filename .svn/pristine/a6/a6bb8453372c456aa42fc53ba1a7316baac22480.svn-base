package com.freeme.filemanager.util.featureoption;
import android.os.SystemProperties;


public class FeatureOption {
    public static final String TYD_FINGERPRINT_SUPPORT = getString("ro.fo_finger_print");
    public static final boolean DROI_FINGERPRINT_SUPPORT = checkSupport("ro.droi_fingerprint_support");
    public static final String PERSIST_FAKE_RAM_SIZE = "persist.fake.ram.size";
    public static final String PERSIST_FAKE_ROM_SIZE = "persist.fake.rom.size";
    public static final String PERSIST_FAKE_USERDATA_SIZE = "persist.fake.userdata.size";
    public static final String PERSIST_FAKE_AVAIL_REAL = "persist.fake.avail.real";
    public static final String PERSIST_FAKE_SHOW_MEM = "persist.fake.show.mem";
    public static final String PERSIST_FAKE_ANIMATION_SIZE = "persist.fake.animation.size";
    public static final String PERSIST_FAKE_FOUR_SIZE = "persist.fake.four.size";
    
    public static String FAKE_RAM_SIZE = getString(PERSIST_FAKE_RAM_SIZE, "2");
    public static String FAKE_ROM_SIZE = getString(PERSIST_FAKE_ROM_SIZE, "16");
    public static String FAKE_USERDATA_SIZE = getString(PERSIST_FAKE_USERDATA_SIZE, "0");
    public static boolean TYD_TOOL_TEST_RAM_1G = getValue("1", FAKE_RAM_SIZE);
    public static boolean TYD_TOOL_TEST_RAM_2G = getValue("2", FAKE_RAM_SIZE);
    public static boolean TYD_TOOL_TEST_RAM_3G = getValue("3", FAKE_RAM_SIZE);    
    public static boolean TYD_TOOL_TEST_RAM_4G = getValue("4", FAKE_RAM_SIZE);
    public static boolean TYD_TOOL_TEST_RAM_6G = getValue("6", FAKE_RAM_SIZE);
    public static boolean TYD_TOOL_TEST_RAM_8G = getValue("8", FAKE_RAM_SIZE);
    public static boolean TYD_TOOL_TEST_RAM_10G = getValue("10", FAKE_RAM_SIZE);
    public static boolean TYD_TOOL_TEST_ROM_4G = false;
    public static boolean TYD_TOOL_TEST_ROM_8G = false;
    public static boolean TYD_TOOL_TEST_ROM_16G = getValue("16", FAKE_ROM_SIZE);
    public static boolean TYD_TOOL_TEST_ROM_32G = getValue("32", FAKE_ROM_SIZE);
    public static boolean TYD_TOOL_TEST_ROM_64G = getValue("64", FAKE_ROM_SIZE);
    public static boolean TYD_TOOL_TEST_ROM_128G = getValue("128", FAKE_ROM_SIZE);
    public static boolean TYD_TOOL_TEST_ROM_256G = getValue("256", FAKE_ROM_SIZE);
    public static boolean TYD_TOOL_TEST_ROM_DUAL_8G = false;
    public static boolean TYD_TOOL_TEST_ROM_DUAL_16G = false;
    public static boolean TYD_TOOL_TEST_USERDATA_2G = false;
    public static boolean TYD_TOOL_TEST_USERDATA_4G = false;
    public static boolean TYD_TOOL_TEST_USERDATA_8G = false;
    public static boolean TYD_TOOL_TEST_USERDATA_16G = getValue("16", FAKE_USERDATA_SIZE);
    public static boolean TYD_TOOL_TEST_USERDATA_32G = getValue("32", FAKE_USERDATA_SIZE);
    public static boolean TYD_TOOL_TEST_USERDATA_64G = getValue("64", FAKE_USERDATA_SIZE);
    public static boolean TYD_TOOL_TEST_USERDATA_128G = getValue("128", FAKE_USERDATA_SIZE);
    public static boolean TYD_TOOL_TEST_USERDATA_256G = getValue("256", FAKE_USERDATA_SIZE);
    public static boolean TYD_TOOL_AVAIL_SIZE_REAL = getValue(PERSIST_FAKE_AVAIL_REAL);
    public static boolean TYD_MMI_SHOW_MEM_INFO = getValue(PERSIST_FAKE_SHOW_MEM);
    public static String FAKE_ANIMATION_SIZE = getString(PERSIST_FAKE_ANIMATION_SIZE, "bootanimation10");
    public static boolean TYD_TOOL_TEST_ANIMATION01 = getValue("bootanimation01", FAKE_ANIMATION_SIZE);
    public static boolean TYD_TOOL_TEST_ANIMATION02 = getValue("bootanimation02", FAKE_ANIMATION_SIZE);
    public static boolean TYD_TOOL_TEST_ANIMATION03 = getValue("bootanimation03", FAKE_ANIMATION_SIZE);    
    public static boolean TYD_TOOL_TEST_ANIMATION04 = getValue("bootanimation04", FAKE_ANIMATION_SIZE);
    public static boolean TYD_TOOL_TEST_ANIMATION05 = getValue("bootanimation05", FAKE_ANIMATION_SIZE);
    public static boolean TYD_TOOL_TEST_ANIMATION06 = getValue("bootanimation06", FAKE_ANIMATION_SIZE);
    public static boolean TYD_TOOL_TEST_ANIMATION07 = getValue("bootanimation07", FAKE_ANIMATION_SIZE);
    public static boolean TYD_TOOL_TEST_ANIMATION08 = getValue("bootanimation08", FAKE_ANIMATION_SIZE);
    public static boolean TYD_TOOL_TEST_ANIMATION09 = getValue("bootanimation09", FAKE_ANIMATION_SIZE);
    public static boolean TYD_TOOL_TEST_ANIMATION10 = getValue("bootanimation10", FAKE_ANIMATION_SIZE);
    public static String FAKE_FOUR_SIZE = getString(PERSIST_FAKE_FOUR_SIZE, "0");
    public static boolean TYD_TOOL_TEST_FOUR = getValue("1", FAKE_FOUR_SIZE);
    public static boolean TYD_TOOL_TEST_TRUE = getValue("0", FAKE_FOUR_SIZE);

    /* get the key's value*/
    private static boolean getValue(String key) {
        return SystemProperties.get(key).equals("1");
    }

     private static String getString(String key) {
        return SystemProperties.get(key);
    }
    
    private static boolean checkSupport(String key){
        return SystemProperties.get(key).equals("1");
    }

    private static String getString(String key, String defStr) {
        return SystemProperties.get(key, defStr);
    }
    
    private static boolean getValue(String size, String fakeSize) {
        return size.equals(fakeSize);
    }
    
    
    public static void setValue(String key, String value) {
        SystemProperties.set(key, value);
    }
}
