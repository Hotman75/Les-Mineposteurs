package com.mineposteurs;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
        private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

        // Configuration pour les coordonnées de la redstone lamp
        public static final ModConfigSpec.IntValue LAMP_X = BUILDER
                        .comment("Coordonnée X de la lampe à surveiller")
                        .defineInRange("lamp_x", -58, Integer.MIN_VALUE, Integer.MAX_VALUE);

        public static final ModConfigSpec.IntValue LAMP_Y = BUILDER
                        .comment("Coordonnée Y de la lampe à surveiller")
                        .defineInRange("lamp_y", 87, -64, 320);

        public static final ModConfigSpec.IntValue LAMP_Z = BUILDER
                        .comment("Coordonnée Z de la lampe à surveiller")
                        .defineInRange("lamp_z", -437, Integer.MIN_VALUE, Integer.MAX_VALUE);

        // Configuration pour les coordonnées du Lectern
        public static final ModConfigSpec.IntValue LECTERN_X = BUILDER
                        .comment("Coordonnée X du Lectern à lire")
                        .defineInRange("lectern_x", -57, Integer.MIN_VALUE, Integer.MAX_VALUE);

        public static final ModConfigSpec.IntValue LECTERN_Y = BUILDER
                        .comment("Coordonnée Y du Lectern à lire")
                        .defineInRange("lectern_y", 89, -64, 320);

        public static final ModConfigSpec.IntValue LECTERN_Z = BUILDER
                        .comment("Coordonnée Z du Lectern à lire")
                        .defineInRange("lectern_z", -436, Integer.MIN_VALUE, Integer.MAX_VALUE);

        public static final ModConfigSpec.IntValue MAX_ACTIONBAR_CHARS = BUILDER
                        .comment("Nombre de caractères max pour l'actionbar")
                        .defineInRange("max_action_bar_chars", 70, 0, 100);

        public static final ModConfigSpec.IntValue TICKS_BETWEEN_PARTS = BUILDER
                        .comment("Ticks entre l'affichage des actionbar")
                        .defineInRange("ticks_between_parts", 100, 0, 2000);

        public static final ModConfigSpec SPEC = BUILDER.build();
}