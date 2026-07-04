package com.semstress.mobile.data

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.google.protobuf.InvalidProtocolBufferException
import com.semstress.mobile.data.proto.PlayerProgressProtoOuterClass.PlayerProgressProto
import java.io.InputStream
import java.io.OutputStream

object PlayerProgressSerializer : Serializer<PlayerProgressProto> {
    override val defaultValue: PlayerProgressProto = PlayerProgressProto.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): PlayerProgressProto {
        return try {
            PlayerProgressProto.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Nao foi possivel ler o progresso persistido.", exception)
        }
    }

    override suspend fun writeTo(t: PlayerProgressProto, output: OutputStream) {
        t.writeTo(output)
    }
}
