package com.novawerk.berlinfoodmap.ui.pages.search

import com.novawerk.berlinfoodmap.domain.restaurant.Tag

/**
 * Search aliases per [Tag], used by [RestaurantSearchBar] so typing a cuisine
 * or format name (e.g. "火锅", "川菜", "hotpot") returns every restaurant
 * carrying that tag.
 *
 * These intentionally duplicate the canonical ZH/EN display names from
 * `values{,-zh}/strings.xml` and add common synonyms the user actually types
 * (e.g. "Szechuan" for Sichuan, "烤肉" for BBQ). `stringResource` is
 * locale-bound and composable-only, so resolving display names that way would
 * miss the other UI language — we always want to match across ZH and EN
 * regardless of which locale the app is in.
 */
private val TAG_SEARCH_ALIASES: Map<Tag, List<String>> = mapOf(
    // Regional cuisines
    Tag.SICHUAN to listOf("川菜", "四川", "Sichuan", "Szechuan"),
    Tag.CANTONESE to listOf("粤菜", "广东", "广式", "港式", "Cantonese", "Guangdong", "Hong Kong"),
    Tag.NORTHERN to listOf("北方菜", "北方", "京菜", "鲁菜", "Northern", "Beijing"),
    Tag.NORTHEASTERN to listOf("东北菜", "东北", "Northeastern", "Dongbei"),
    Tag.SHANGHAINESE to listOf("江浙沪", "上海", "江浙", "本帮", "Shanghainese", "Shanghai"),
    Tag.HUNAN to listOf("湘菜", "湖南", "Hunan"),
    Tag.XINJIANG to listOf("新疆菜", "新疆", "Xinjiang", "Uyghur"),
    Tag.TAIWANESE to listOf("台菜", "台湾", "台式", "Taiwanese", "Taiwan"),
    Tag.MUSLIM to listOf("清真", "穆斯林", "Halal", "Muslim"),
    Tag.MONGOLIAN to listOf("蒙古烤肉", "蒙古", "Mongolian", "Mongolisch"),

    // Formats
    Tag.HOTPOT to listOf("火锅", "Hotpot", "Hot Pot"),
    Tag.BBQ to listOf("烧烤", "烤肉", "串", "BBQ", "Grill", "Barbecue"),
    Tag.NOODLES to listOf("面食", "面条", "拉面", "刀削", "Noodles", "Noodle"),
    Tag.DUMPLINGS to listOf("饺子", "包子", "Dumplings", "Dumpling"),
    Tag.DIM_SUM to listOf("点心", "茶点", "Dim Sum", "Yum Cha"),
    Tag.MALATANG to listOf("麻辣烫", "麻辣", "Malatang"),
    Tag.VEGETARIAN to listOf("素食", "素菜", "Vegetarian", "Vegan", "Veggie"),
    Tag.BREAKFAST to listOf("早餐", "早点", "Breakfast"),
    Tag.TEA_HOUSE to listOf("茶寮", "茶馆", "茶室", "Tea House", "Tea"),
    Tag.BAKERY to listOf("烘焙", "面包", "蛋糕", "Bakery"),
    Tag.STREET_FOOD to listOf("小吃", "街边", "Street Food", "Snacks"),
    Tag.FUSION to listOf("创新中餐", "融合", "新中餐", "Modern Chinese", "Fusion"),
)

/**
 * Returns every [Tag] whose alias list contains [trimmedQuery]
 * (case-insensitive substring match). Empty result if [trimmedQuery] is blank.
 */
internal fun matchTagsForQuery(trimmedQuery: String): Set<Tag> {
    if (trimmedQuery.isEmpty()) return emptySet()
    val out = mutableSetOf<Tag>()
    for ((tag, aliases) in TAG_SEARCH_ALIASES) {
        if (aliases.any { it.contains(trimmedQuery, ignoreCase = true) }) {
            out += tag
        }
    }
    return out
}
