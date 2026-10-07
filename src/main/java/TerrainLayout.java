import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

/** Ground footprints of the scenery painted into the repeating grass tile. */
public final class TerrainLayout {
    public static final int TILE_WIDTH = 1536;
    public static final int TILE_HEIGHT = 1024;
    public static final int SPAWN_TILE_X = 640;
    public static final int SPAWN_TILE_Y = 360;

    private TerrainLayout() { }

    /**
     * Coordinates are pixels in grasstile.png, before camera translation.
     * Walls use their foundations and trees their trunks, not their overhead
     * branches. Arch openings and decorative grass remain walkable.
     */
    public static List<Shape> obstacles() {
        List<Shape> obstacles = new ArrayList<>();

        // Raised, impassable cliff and chasm scenery at the upper tile edges.
        obstacles.add(polygon(0, 0, 351, 0, 350, 29, 289, 48, 264, 75,
                213, 91, 200, 118, 159, 132, 144, 171, 111, 181,
                88, 169, 53, 178, 0, 175));
        obstacles.add(polygon(1035, 0, 1536, 0, 1536, 309, 1490, 302,
                1462, 282, 1434, 304, 1397, 284, 1374, 274,
                1347, 292, 1307, 282, 1277, 259, 1286, 227,
                1252, 213, 1224, 194, 1226, 167, 1192, 149,
                1153, 154, 1120, 131, 1115, 87, 1068, 71, 1049, 41));

        // Chasm cutouts on the right edge and the two lower corners.
        obstacles.add(polygon(1536, 575, 1507, 580, 1479, 598, 1444, 593,
                1417, 610, 1426, 642, 1421, 667, 1439, 684,
                1469, 694, 1505, 681, 1536, 693));
        obstacles.add(polygon(1536, 777, 1505, 786, 1512, 813, 1483, 834,
                1451, 844, 1434, 869, 1400, 882, 1358, 894,
                1325, 919, 1303, 936, 1283, 966, 1298, 982,
                1353, 990, 1393, 980, 1424, 997, 1452, 989,
                1497, 963, 1536, 955));
        obstacles.add(polygon(0, 772, 20, 787, 19, 824, 35, 853,
                62, 873, 103, 884, 131, 907, 175, 922,
                206, 939, 237, 949, 248, 974, 278, 991,
                309, 1005, 316, 1024, 0, 1024));

        // Northwest arch: separate feet preserve its central doorway.
        obstacles.add(rectangle(204, 194, 27, 22));
        obstacles.add(rectangle(271, 178, 23, 22));
        obstacles.add(rectangle(297, 155, 24, 22));
        obstacles.add(rectangle(325, 85, 20, 17));
        obstacles.add(polygon(319, 150, 340, 140, 363, 155, 363, 174,
                344, 185, 318, 169));
        obstacles.add(rectangle(365, 155, 20, 17));
        obstacles.add(ellipse(343, 192, 33, 21));

        // Northern broken masonry and crystal cluster.
        obstacles.add(polygon(684, 165, 711, 153, 747, 171, 773, 184,
                753, 197, 716, 183));
        obstacles.add(ellipse(799, 156, 33, 22));
        obstacles.add(polygon(1037, 100, 1062, 87, 1090, 92,
                1075, 108, 1052, 117));

        // Western freestanding column, and the broken wall west of spawn.
        obstacles.add(polygon(179, 394, 197, 384, 219, 393,
                220, 408, 198, 421, 176, 410));
        obstacles.add(ellipse(246, 408, 26, 16));
        obstacles.add(polygon(499, 398, 521, 389, 539, 393, 541, 386,
                565, 383, 577, 400, 563, 417, 580, 427,
                562, 437, 539, 426, 527, 416, 506, 416));
        obstacles.add(rectangle(591, 395, 23, 23));

        // Northeast column pair, low wall, and the large eastern arch.
        obstacles.add(rectangle(1117, 287, 22, 21));
        obstacles.add(rectangle(1146, 305, 25, 21));
        obstacles.add(polygon(1080, 316, 1101, 305, 1138, 326,
                1168, 311, 1190, 321, 1154, 346, 1113, 340));
        obstacles.add(ellipse(1059, 315, 22, 17));
        obstacles.add(rectangle(1180, 403, 20, 23));
        obstacles.add(rectangle(1228, 403, 23, 24));
        obstacles.add(polygon(1252, 411, 1273, 399, 1306, 417,
                1304, 433, 1285, 446, 1253, 431));
        obstacles.add(rectangle(1308, 409, 20, 22));
        obstacles.add(ellipse(1228, 438, 26, 18));
        obstacles.add(rectangle(1467, 362, 25, 21));

        // Loose stone blocks around the centre of the tile.
        obstacles.add(polygon(304, 530, 329, 516, 352, 526,
                333, 546, 310, 543));
        obstacles.add(ellipse(351, 503, 23, 14));
        obstacles.add(polygon(917, 574, 936, 565, 963, 579,
                960, 591, 942, 599, 919, 587));
        obstacles.add(polygon(906, 610, 928, 597, 970, 617,
                954, 634, 928, 623));
        obstacles.add(ellipse(967, 589, 23, 15));
        obstacles.add(polygon(1395, 534, 1422, 521, 1468, 544,
                1470, 558, 1447, 574, 1424, 562));
        obstacles.add(polygon(1320, 603, 1345, 590, 1367, 600,
                1345, 617));
        obstacles.add(polygon(87, 626, 110, 613, 133, 626,
                112, 641));
        obstacles.add(polygon(509, 720, 538, 707, 561, 720,
                545, 735, 518, 735));
        obstacles.add(polygon(864, 779, 884, 768, 904, 779,
                925, 765, 944, 776, 924, 789, 907, 788, 888, 791));

        // Southwest twin arches: three separated foundations and side posts.
        obstacles.add(rectangle(124, 823, 28, 24));
        obstacles.add(rectangle(192, 833, 29, 22));
        obstacles.add(rectangle(232, 817, 19, 27));
        obstacles.add(rectangle(278, 812, 26, 25));
        obstacles.add(rectangle(309, 810, 23, 21));
        obstacles.add(rectangle(272, 734, 22, 20));
        obstacles.add(ellipse(205, 847, 28, 21));
        obstacles.add(polygon(421, 810, 444, 797, 471, 812, 493, 814,
                500, 831, 478, 845, 455, 834, 429, 834));
        obstacles.add(ellipse(408, 838, 27, 24));

        // Southern rubble and southeast arch foundations.
        obstacles.add(polygon(674, 878, 693, 867, 724, 883, 735, 901,
                733, 918, 712, 931, 681, 917, 659, 939,
                630, 954, 616, 947, 637, 930, 662, 916, 673, 899));
        obstacles.add(polygon(641, 945, 666, 931, 692, 942,
                677, 958, 652, 967, 633, 959));
        obstacles.add(rectangle(741, 900, 26, 21));
        obstacles.add(ellipse(621, 899, 21, 17));
        obstacles.add(polygon(375, 1006, 396, 995, 419, 1007,
                399, 1022));
        obstacles.add(rectangle(1235, 848, 24, 24));
        obstacles.add(rectangle(1299, 873, 34, 22));
        obstacles.add(rectangle(1260, 790, 23, 20));
        obstacles.add(ellipse(1198, 842, 32, 22));

        // Tree roots only: branches and foliage do not form invisible walls.
        double[][] trunks = {
            {22, 278, 17, 13}, {165, 205, 17, 12},
            {480, 134, 21, 14}, {778, 151, 18, 13},
            {1018, 319, 24, 17}, {157, 404, 19, 14},
            {47, 460, 23, 17}, {161, 583, 21, 15},
            {951, 551, 22, 15}, {1502, 535, 21, 16},
            {214, 750, 29, 20}, {620, 888, 24, 16},
            {1190, 809, 25, 18}, {1394, 779, 31, 21},
            {333, 991, 23, 16}, {1020, 999, 24, 16}
        };
        for (double[] trunk : trunks) {
            obstacles.add(ellipse(trunk[0] - trunk[2], trunk[1] - trunk[3],
                    trunk[2] * 2, trunk[3] * 2));
        }
        return List.copyOf(obstacles);
    }

    private static Shape rectangle(double x, double y, double width, double height) {
        return new Rectangle2D.Double(x, y, width, height);
    }

    private static Shape ellipse(double x, double y, double width, double height) {
        return new Ellipse2D.Double(x, y, width, height);
    }

    private static Shape polygon(double... coordinates) {
        Path2D.Double shape = new Path2D.Double();
        shape.moveTo(coordinates[0], coordinates[1]);
        for (int index = 2; index < coordinates.length; index += 2) {
            shape.lineTo(coordinates[index], coordinates[index + 1]);
        }
        shape.closePath();
        return shape;
    }
}
