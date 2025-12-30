package com.altarfunds.mobile.data

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    
    @TypeConverter
    fun fromStringList(value: List<String>?): String? {
        return Gson().toJson(value)
    }
    
    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        return Gson().fromJson(value, object : TypeToken<List<String>>() {}.type)
    }
    
    @TypeConverter
    fun fromStringMap(value: Map<String, String>?): String? {
        return Gson().toJson(value)
    }
    
    @TypeConverter
    fun toStringMap(value: String?): Map<String, String>? {
        return Gson().fromJson(value, object : TypeToken<Map<String, String>>() {}.type)
    }
    
    @TypeConverter
    fun fromAnyMap(value: Map<String, Any>?): String? {
        return Gson().toJson(value)
    }
    
    @TypeConverter
    fun toAnyMap(value: String?): Map<String, Any>? {
        return Gson().fromJson(value, object : TypeToken<Map<String, Any>>() {}.type)
    }
}
