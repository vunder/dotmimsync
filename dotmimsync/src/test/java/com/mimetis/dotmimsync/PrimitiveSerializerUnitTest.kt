package com.mimetis.dotmimsync

import android.util.Base64
import com.mimetis.dotmim.sync.PrimitiveSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.encodeToJsonElement
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner
import java.util.*

@RunWith(PowerMockRunner::class)
@PrepareForTest(Base64::class)
class PrimitiveSerializerUnitTest {
    @Test
    fun `should return String for base64`() {
        val data = arrayOf<Byte>(1, 2, 3, 4, 5, 6, 7, 8, 9, 10).toByteArray()
        val input = "AQIDBAUGBwgJCg=="
        val decoderMock = Mockito.mock(JsonDecoder::class.java)
        val element = Json.encodeToJsonElement(input)
        Mockito.`when`(decoderMock.decodeJsonElement()).thenReturn(element)

        PowerMockito.mockStatic(Base64::class.java)
        Mockito.`when`(Base64.decode(ArgumentMatchers.anyString(), ArgumentMatchers.anyInt())).thenReturn(data)

        val result = PrimitiveSerializer.deserialize(decoderMock)
        Assert.assertTrue(result is String)
        Assert.assertEquals(input, result)
    }

    @Test
    fun `should return String for guid`() {
        val uuid = UUID.randomUUID()
        val decoderMock = Mockito.mock(JsonDecoder::class.java)
        val element = Json.encodeToJsonElement(uuid.toString().lowercase())
        Mockito.`when`(decoderMock.decodeJsonElement()).thenReturn(element)

        val result = PrimitiveSerializer.deserialize(decoderMock)
        Assert.assertTrue(result is String)
        Assert.assertEquals(uuid.toString(), result)
    }

    @Test
    fun `should return Boolean=true`() {
        val decoderMock = Mockito.mock(JsonDecoder::class.java)
        val element = Json.encodeToJsonElement(true)
        Mockito.`when`(decoderMock.decodeJsonElement()).thenReturn(element)

        val result = PrimitiveSerializer.deserialize(decoderMock)
        Assert.assertTrue(result is Boolean)
        Assert.assertTrue(result as Boolean)
    }

    @Test
    fun `should return Boolean=false`() {
        val decoderMock = Mockito.mock(JsonDecoder::class.java)
        val element = Json.encodeToJsonElement(false)
        Mockito.`when`(decoderMock.decodeJsonElement()).thenReturn(element)

        val result = PrimitiveSerializer.deserialize(decoderMock)
        Assert.assertTrue(result is Boolean)
        Assert.assertFalse(result as Boolean)
    }

    @Test
    fun `should return Int`() {
        val decoderMock = Mockito.mock(JsonDecoder::class.java)
        val element = Json.encodeToJsonElement(14)
        Mockito.`when`(decoderMock.decodeJsonElement()).thenReturn(element)

        val result = PrimitiveSerializer.deserialize(decoderMock)
        Assert.assertTrue(result is Int)
        Assert.assertEquals(14, result)
    }

    @Test
    fun `should return Int for small Long`() {
        val decoderMock = Mockito.mock(JsonDecoder::class.java)
        val element = Json.encodeToJsonElement(500L)
        Mockito.`when`(decoderMock.decodeJsonElement()).thenReturn(element)

        val result = PrimitiveSerializer.deserialize(decoderMock)
        Assert.assertTrue(result is Int)
        Assert.assertEquals(500, result)
    }

    @Test
    fun `should return Long`() {
        val decoderMock = Mockito.mock(JsonDecoder::class.java)
        val element = Json.encodeToJsonElement(Int.MAX_VALUE.toLong() + 1L)
        Mockito.`when`(decoderMock.decodeJsonElement()).thenReturn(element)

        val result = PrimitiveSerializer.deserialize(decoderMock)
        Assert.assertTrue(result is Long)
        Assert.assertEquals(Int.MAX_VALUE.toLong() + 1L, result)
    }

    @Test
    fun `should return Double`() {
        val decoderMock = Mockito.mock(JsonDecoder::class.java)
        val element = Json.encodeToJsonElement(2.6)
        Mockito.`when`(decoderMock.decodeJsonElement()).thenReturn(element)

        val result = PrimitiveSerializer.deserialize(decoderMock)
        Assert.assertTrue(result is Double)
        Assert.assertEquals(2.6, result)
    }

    @Test
    fun `should return Double for Float`() {
        val decoderMock = Mockito.mock(JsonDecoder::class.java)
        val element = Json.encodeToJsonElement(1.79f)
        Mockito.`when`(decoderMock.decodeJsonElement()).thenReturn(element)

        val result = PrimitiveSerializer.deserialize(decoderMock)
        Assert.assertTrue(result is Double)
        Assert.assertEquals(1.79, result)
    }

    @Test
    fun `should return String`() {
        val decoderMock = Mockito.mock(JsonDecoder::class.java)
        val element = Json.encodeToJsonElement("Foo")
        Mockito.`when`(decoderMock.decodeJsonElement()).thenReturn(element)

        val result = PrimitiveSerializer.deserialize(decoderMock)
        Assert.assertTrue(result is String)
        Assert.assertEquals("Foo", result)
    }

    @Test
    fun `should return String for multiline (n) text starts with number`() {
        val decoderMock = Mockito.mock(JsonDecoder::class.java)
        val element = Json.encodeToJsonElement("123\nfff")
        Mockito.`when`(decoderMock.decodeJsonElement()).thenReturn(element)

        val result = PrimitiveSerializer.deserialize(decoderMock)
        Assert.assertTrue(result is String)
    }

    @Test
    fun `should return String for multiline (r) text starts with number1`() {
        val decoderMock = Mockito.mock(JsonDecoder::class.java)
        val element = Json.encodeToJsonElement("123\rfff")
        Mockito.`when`(decoderMock.decodeJsonElement()).thenReturn(element)

        val result = PrimitiveSerializer.deserialize(decoderMock)
        Assert.assertTrue(result is String)
    }

    @Test
    fun `should return String for multiline (rn) text starts with number`() {
        val decoderMock = Mockito.mock(JsonDecoder::class.java)
        val element = Json.encodeToJsonElement("123\r\nfff")
        Mockito.`when`(decoderMock.decodeJsonElement()).thenReturn(element)

        val result = PrimitiveSerializer.deserialize(decoderMock)
        Assert.assertTrue(result is String)
    }
}