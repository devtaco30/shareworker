package com.devtaco.shareworker.utils

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.common.serialization.Serializer

/**
 * 기본적으로 [KotlinJsonAdapterFactory] 는 선언해 두고,
 *
 * 날짜 변환 아답터만 다르게 구현해서 사용하도록 한다.
 */
object MoshiBuilder {
    fun moshi(): Moshi {
        return Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }
}

// Moshi를 활용한 JSON 직렬화 및 역직렬화 클래스 정의
open class MoshiSerializer<T>(clazz: Class<T>) : Serializer<T> {
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val adapter = moshi.adapter(clazz)

    override fun serialize(topic: String?, data: T?): ByteArray? {
        return data?.let { adapter.toJson(it).toByteArray() }
    }
}

open class MoshiDeserializer<T>(clazz: Class<T>) : Deserializer<T> {
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val adapter = moshi.adapter(clazz)


    override fun deserialize(topic: String?, data: ByteArray?): T? {
        // null 값이 실제로는 빈 바이트 배열(byte[0])로 전송되는 경우가 있다.
        // 이는 Kafka 프로토콜에서 null 값을 전송할 때 데이터가 완전히 없는 대신,
        // 길이가 0인 바이트 배열로 전달되기 때문.
        // 따라서 data == null 조건만으로는 tombstone 메시지를 인식하지 못하고,
        // data.isEmpty() 조건을 추가해서 빈 바이트 배열로 전송된 tombstone 메시지를 제대로 처리할 수 있게 한다.
        return if (data == null || data.isEmpty()) null else adapter.fromJson(String(data))
    }
}

// Moshi 기반 커스텀 Serde 생성 함수
inline fun <reified T> moshiSerde(): Serde<T> {
    return Serdes.serdeFrom(MoshiSerializer(T::class.java), MoshiDeserializer(T::class.java))
}