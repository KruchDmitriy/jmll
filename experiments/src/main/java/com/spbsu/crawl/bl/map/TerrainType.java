package com.spbsu.crawl.bl.map;


import com.spbsu.commons.util.ArrayTools;
import com.spbsu.crawl.data.impl.UpdateMapCellMessage;

public enum TerrainType {
  CLOSED_DOOR(new CodeRange(1)),
  OPEN_DOOR(new CodeRange(34)),
  RUINED_DOOR(new CodeRange(2)),
  SEALED_DOOR(new CodeRange(3)),
  TREE(new CodeRange(4)),
  WALL(new CodeRange(5, 6, 7, 8, 9, 10, 11, 12, 13)),
  GRATE(new CodeRange(14)),
  OPEN_SEA(new CodeRange(15)),
  LAVA_SEA(new CodeRange(16)),
  STATUE(new CodeRange(17, 18)),
  MALIGN_GATEWAY(new CodeRange(19)),
  LAVA(new CodeRange(30)),
  DEEP_WATER(new CodeRange(31)),
  SHALLOW_WATER(new CodeRange(32)),
  FLOOR(new CodeRange(33)),
  TRAP(new CodeRange(35, 36, 37, 38, 39)),
  ENTER_SHOP(new CodeRange(40)),
  ABANDONED_SHOP(new CodeRange(41)),
  STONE_STAIR_DOWN(new CodeRange(42, 43, 44, 45)),
  STONE_STAIR_UP(new CodeRange(46, 47, 48, 49)),
  ENTRANCE(new CodeRange(ArrayTools.sequence(50, 55))),
  EXIT(new CodeRange(55, 60, 61, 62)),
  STONE_ARCH(new CodeRange(56)),
  EXIT_DUNGEON(new CodeRange(60)),
  UNKNOWN_TYPE(new CodeRange(-1));

  private final CodeRange code;

  TerrainType(CodeRange code) {
    this.code = code;
  }

  public boolean contains(int idx) {
    return code.contains(idx);
  }

  public static TerrainType fromMessage(final UpdateMapCellMessage cellMessage) {
    final int dungeonFeature = cellMessage.getDungeonFeatureType();
    for (TerrainType type : TerrainType.values()) {
      if (type.contains(dungeonFeature)) {
        return type;
      }
    }
    return TerrainType.UNKNOWN_TYPE;
  }

}

//
//"key" "value"
//        "DNGN_UNSEEN" 0
//        "DNGN_CLOSED_DOOR" 1
//        "DNGN_RUNED_DOOR" 2
//        "DNGN_SEALED_DOOR" 3
//        "DNGN_TREE" 4
//        "DNGN_METAL_WALL" 5
//        "DNGN_CRYSTAL_WALL" 6
//        "DNGN_ROCK_WALL" 7
//        "DNGN_SLIMY_WALL" 8
//        "DNGN_STONE_WALL" 9
//        "DNGN_PERMAROCK_WALL" 10
//        "DNGN_CLEAR_ROCK_WALL" 11
//        "DNGN_CLEAR_STONE_WALL" 12
//        "DNGN_CLEAR_PERMAROCK_WALL" 13
//        "DNGN_GRATE" 14
//        "DNGN_OPEN_SEA" 15
//        "DNGN_LAVA_SEA" 16
//        "DNGN_ORCISH_IDOL" 17
//        "DNGN_GRANITE_STATUE" 18
//        "DNGN_MALIGN_GATEWAY" 19
//        "DNGN_LAVA" 30
//        "DNGN_DEEP_WATER" 31
//        "DNGN_SHALLOW_WATER" 32
//        "DNGN_FLOOR" 33
//        "DNGN_OPEN_DOOR" 34
//        "DNGN_TRAP_MECHANICAL" 35
//        "DNGN_TRAP_TELEPORT" 36
//        "DNGN_TRAP_SHAFT" 37
//        "DNGN_TRAP_WEB" 38
//        "DNGN_UNDISCOVERED_TRAP" 39
//        "DNGN_ENTER_SHOP" 40
//        "DNGN_ABANDONED_SHOP" 41
//        "DNGN_STONE_STAIRS_DOWN_I" 42
//        "DNGN_STONE_STAIRS_DOWN_II" 43
//        "DNGN_STONE_STAIRS_DOWN_III" 44
//        "DNGN_ESCAPE_HATCH_DOWN" 45
//        "DNGN_STONE_STAIRS_UP_I" 46
//        "DNGN_STONE_STAIRS_UP_II" 47
//        "DNGN_STONE_STAIRS_UP_III" 48
//        "DNGN_ESCAPE_HATCH_UP" 49
//        "DNGN_ENTER_DIS" 50
//        "DNGN_ENTER_GEHENNA" 51
//        "DNGN_ENTER_COCYTUS" 52
//        "DNGN_ENTER_TARTARUS" 53
//        "DNGN_ENTER_ABYSS" 54
//        "DNGN_EXIT_ABYSS" 55
//        "DNGN_STONE_ARCH" 56
//        "DNGN_ENTER_PANDEMONIUM" 57
//        "DNGN_EXIT_PANDEMONIUM" 58
//        "DNGN_TRANSIT_PANDEMONIUM" 59
//        "DNGN_EXIT_DUNGEON" 60
//        "DNGN_EXIT_THROUGH_ABYSS" 61
//        "DNGN_EXIT_HELL" 62
//        "DNGN_ENTER_HELL" 63
//        "DNGN_ENTER_LABYRINTH" 64
//        "DNGN_TELEPORTER" 65
//        "DNGN_ENTER_PORTAL_VAULT" 66
//        "DNGN_EXIT_PORTAL_VAULT" 67
//        "DNGN_EXPIRED_PORTAL" 68
//        "DNGN_ENTER_DWARF" 69
//        "DNGN_ENTER_ORC" 70
//        "DNGN_ENTER_LAIR" 71
//        "DNGN_ENTER_SLIME" 72
//        "DNGN_ENTER_VAULTS" 73
//        "DNGN_ENTER_CRYPT" 74
//        "DNGN_ENTER_BLADE" 75
//        "DNGN_ENTER_ZOT" 76
//        "DNGN_ENTER_TEMPLE" 77
//        "DNGN_ENTER_SNAKE" 78
//        "DNGN_ENTER_ELF" 79
//        "DNGN_ENTER_TOMB" 80
//        "DNGN_ENTER_SWAMP" 81
//        "DNGN_ENTER_SHOALS" 82
//        "DNGN_ENTER_SPIDER" 83
//        "DNGN_ENTER_FOREST" 84
//        "DNGN_ENTER_DEPTHS" 85
//        "DNGN_EXIT_DWARF" 86
//        "DNGN_EXIT_ORC" 87
//        "DNGN_EXIT_LAIR" 88
//        "DNGN_EXIT_SLIME" 89
//        "DNGN_EXIT_VAULTS" 90
//        "DNGN_EXIT_CRYPT" 91
//        "DNGN_EXIT_BLADE" 92
//        "DNGN_EXIT_ZOT" 93
//        "DNGN_EXIT_TEMPLE" 94
//        "DNGN_EXIT_SNAKE" 95
//        "DNGN_EXIT_ELF" 96
//        "DNGN_EXIT_TOMB" 97
//        "DNGN_EXIT_SWAMP" 98
//        "DNGN_EXIT_SHOALS" 99
//        "DNGN_EXIT_SPIDER" 100
//        "DNGN_EXIT_FOREST" 101
//        "DNGN_EXIT_DEPTHS" 102
//        "DNGN_ALTAR_ZIN" 103
//        "DNGN_ALTAR_SHINING_ONE" 104
//        "DNGN_ALTAR_KIKUBAAQUDGHA" 105
//        "DNGN_ALTAR_YREDELEMNUL" 106
//        "DNGN_ALTAR_XOM" 107
//        "DNGN_ALTAR_VEHUMET" 108
//        "DNGN_ALTAR_OKAWARU" 109
//        "DNGN_ALTAR_MAKHLEB" 110
//        "DNGN_ALTAR_SIF_MUNA" 111
//        "DNGN_ALTAR_TROG" 112
//        "DNGN_ALTAR_NEMELEX_XOBEH" 113
//        "DNGN_ALTAR_ELYVILON" 114
//        "DNGN_ALTAR_LUGONU" 115
//        "DNGN_ALTAR_BEOGH" 116
//        "DNGN_ALTAR_JIYVA" 117
//        "DNGN_ALTAR_FEDHAS" 118
//        "DNGN_ALTAR_CHEIBRIADOS" 119
//        "DNGN_ALTAR_ASHENZARI" 120
//        "DNGN_ALTAR_DITHMENOS" 121
//        "DNGN_FOUNTAIN_BLUE" 122
//        "DNGN_FOUNTAIN_SPARKLING" 123
//        "DNGN_FOUNTAIN_BLOOD" 124
//        "DNGN_DRY_FOUNTAIN_BLUE" 125
//        "DNGN_DRY_FOUNTAIN_SPARKLING" 126
//        "DNGN_DRY_FOUNTAIN_BLOOD" 127
//        "DNGN_DRY_FOUNTAIN" 128
//        "DNGN_EXPLORE_HORIZON" 129
//        "DNGN_UNKNOWN_ALTAR" 130
//        "DNGN_UNKNOWN_PORTAL" 131
//        "DNGN_ABYSSAL_STAIR" 132
//        "DNGN_BADLY_SEALED_DOOR" 133
//        "DNGN_SEALED_STAIRS_UP" 134
//        "DNGN_SEALED_STAIRS_DOWN" 135
//        "DNGN_TRAP_ALARM" 136
//        "DNGN_TRAP_ZOT" 137
//        "DNGN_PASSAGE_OF_GOLUBRIA" 138
//        "DNGN_ENTER_ZIGGURAT" 139
//        "DNGN_ENTER_BAZAAR" 140
//        "DNGN_ENTER_TROVE" 141
//        "DNGN_ENTER_SEWER" 142
//        "DNGN_ENTER_OSSUARY" 143
//        "DNGN_ENTER_BAILEY" 144
//        "DNGN_ENTER_ICE_CAVE" 145
//        "DNGN_ENTER_VOLCANO" 146
//        "DNGN_ENTER_WIZLAB" 147
//        "DNGN_UNUSED_ENTER_PORTAL_1" 148
//        "DNGN_EXIT_ZIGGURAT" 149
//        "DNGN_EXIT_BAZAAR" 150
//        "DNGN_EXIT_TROVE" 151
//        "DNGN_EXIT_SEWER" 152
//        "DNGN_EXIT_OSSUARY" 153
//        "DNGN_EXIT_BAILEY" 154
//        "DNGN_EXIT_ICE_CAVE" 155
//        "DNGN_EXIT_VOLCANO" 156
//        "DNGN_EXIT_WIZLAB" 157
//        "DNGN_EXIT_LABYRINTH" 158
//        "DNGN_UNUSED_EXIT_PORTAL_1" 159
//        "DNGN_ALTAR_GOZAG" 160
//        "DNGN_ALTAR_QAZLAL" 161
//        "DNGN_ALTAR_RU" 162
//        "DNGN_TRAP_SHADOW" 163
//        "DNGN_TRAP_SHADOW_DORMANT" 164
//        "DNGN_ALTAR_ECUMENICAL" 165
//        "DNGN_ALTAR_PAKELLAS" 166
//        "NUM_FEATURES" 167
