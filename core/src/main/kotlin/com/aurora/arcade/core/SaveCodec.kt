package com.aurora.arcade.core

import java.io.*
import java.util.zip.CRC32

/** Bounded, versioned binary format. No Java object deserialization. */
object SaveCodec {
    fun encode(save: EngineSave): ByteArray {
        val bytes = ByteArrayOutputStream()
        DataOutputStream(bytes).use { out ->
            out.writeInt(0x41555241); out.writeInt(1); out.writeInt(save.mode.ordinal)
            writeSnapshot(out,save.current)
            out.writeBoolean(save.undo != null)
            save.undo?.let { writeSnapshot(out,it) }
        }
        val payload = bytes.toByteArray()
        return ByteArrayOutputStream().also { buffer ->
            buffer.write(payload)
            DataOutputStream(buffer).writeLong(CRC32().apply { update(payload) }.value)
        }.toByteArray()
    }
    fun decode(bytes: ByteArray): EngineSave? = runCatching {
        require(bytes.size in 32..16_384)
        val payload = bytes.copyOf(bytes.size-8)
        val checksum = DataInputStream(ByteArrayInputStream(bytes,bytes.size-8,8)).readLong()
        require(checksum == CRC32().apply { update(payload) }.value)
        DataInputStream(ByteArrayInputStream(payload)).use { input ->
            require(input.readInt() == 0x41555241 && input.readInt() == 1)
            val mode = Mode.entries[input.readInt()]
            val current = readSnapshot(input)
            val undo = if (input.readBoolean()) readSnapshot(input) else null
            require(input.available() == 0)
            val save = EngineSave(mode,current,undo)
            require(current.state.gameOver || GameEngine.restore(save).fits(current.state.active))
            if (undo != null) require(!undo.state.gameOver && GameEngine.restore(EngineSave(mode,undo)).fits(undo.state.active))
            save
        }
    }.getOrNull()
    private fun writePiece(out: DataOutputStream, p: Piece) {
        out.writeInt(p.kind.ordinal); out.writeInt(p.x); out.writeInt(p.y); out.writeInt(p.rotation)
    }
    private fun readPiece(input: DataInputStream): Piece {
        val piece = Piece(Kind.entries[input.readInt()],input.readInt(),input.readInt(),input.readInt())
        require(piece.x in -3..9 && piece.y in 0..23 && piece.rotation in 0..3)
        return piece
    }
    private fun writeSnapshot(out: DataOutputStream, snap: EngineSnapshot) {
        val s = snap.state
        s.board.forEach { out.writeByte(it) }; writePiece(out,s.active)
        out.writeInt(s.next.size); s.next.forEach { out.writeInt(it.ordinal) }
        out.writeInt(s.held?.ordinal ?: -1); out.writeBoolean(s.holdUsed)
        out.writeInt(s.score); out.writeInt(s.lines); out.writeInt(s.combo)
        out.writeBoolean(s.gameOver); out.writeInt(s.undoCount); out.writeLong(s.pieceId)
        out.writeLong(snap.randomState); out.writeLong(snap.fallMs); out.writeLong(snap.lockMs)
        out.writeInt(snap.lockResets); out.writeInt(snap.dropPoints)
        out.writeBoolean(snap.recallFrom != null); snap.recallFrom?.let { writePiece(out,it) }
    }
    private fun readSnapshot(input: DataInputStream): EngineSnapshot {
        val board = List(240) { input.readUnsignedByte().also { require(it in 0..7) } }
        val active = readPiece(input)
        val count = input.readInt().also { require(it in 5..13) }
        val next = List(count) { Kind.entries[input.readInt()] }
        val heldIndex = input.readInt().also { require(it in -1..6) }
        val holdUsed = input.readBoolean()
        val score = input.readInt().also { require(it >= 0) }
        val lines = input.readInt().also { require(it in 0..10_000_000) }
        val combo = input.readInt().also { require(it in -1..10_000_000) }
        val over = input.readBoolean()
        val undoCount = input.readInt().also { require(it >= 0) }
        val pieceId = input.readLong().also { require(it >= 0) }
        val random = input.readLong().also { require(it != 0L) }
        val fall = input.readLong().also { require(it in 0..800) }
        val lock = input.readLong().also { require(it in 0..500) }
        val resets = input.readInt().also { require(it in 0..15) }
        val drop = input.readInt().also { require(it in 0..1000) }
        val recall = if(input.readBoolean()) readPiece(input) else null
        return EngineSnapshot(GameState(board,active,next,if(heldIndex<0)null else Kind.entries[heldIndex],
            holdUsed,score,lines,combo,over,undoCount,pieceId),random,fall,lock,resets,drop,recall)
    }
}
