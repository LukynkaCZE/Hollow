# Hollow

Simple wrapper around SQLite for making local databases and caches fast and easily.

Hollow uses the memory-first principal meaning when you add, change or remove objects from cache, they will be updated in memory first and then database job will be pushed to queue and processed later

## Installation

<img src="https://cdn.worldvectorlogo.com/logos/kotlin-2.svg" width="16px"></img>
**Kotlin DSL**

```kotlin
repositories {
    maven("https://mvn.devos.one/releases")
}

dependencies {
    implementation("cz.lukynka:hollow:1.0")
}
```

## Usage

Hollow is designed to be very simple to use. You can create a hollow cache by extending the HollowChache<T> abstract
class:

```kotlin
object PlayerDataCache : HollowCache<PlayerData>("player_data") {

    override fun serialize(value: PlayerData): String {
        return Json.encodeToString<PlayerData>(value)
    }

    override fun deserialize(string: String): PlayerData {
        return Json.decodeFromString<PlayerData>(string)
    }

}

data class PlayerData(
    val username: String,
    val uuid: String,
    var level: Int,
    var isAdmin: Boolean
)
```

_Note that you can use your own serialization to string, I'm just using kotlinx-serialization for convenience_

Then you need to initialize the Hollow database and your caches:

```kotlin
Hollow.initialize("cool-database")
PlayerDataCache.initialize()
```

After that, you can freely add, remove or edit objects:

```kotlin
//add new player
val playerUUID = UUID.randomUUID()
val playerData = PlayerData("LukynkaCZE", playerUUID.toString(), 0, true)
PlayerDataCache[playerUUID] = playerData

//edit player
val player = PlayerDataCache[playerUUID]
player.level = 2
PlayerDataCache[playerUUID] = player

//remove
PlayerDataCache.remove(playerUUID)

//remove all
PlayerDataCache.clear()
```

_Note that all hollow caches are UUID key based and icba changing. Made this for my projects specifically where I know Im gonna need just uuid keys. If you want to expand it tho feel free to submit pr!_