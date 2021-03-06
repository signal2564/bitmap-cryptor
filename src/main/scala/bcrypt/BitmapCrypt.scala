package bcrypt

import scala.io._
import java.io.File
import java.io.PrintWriter
import java.lang.Exception

object BitmapCrypt {

    def crypt(dataFile: String, secretKey: String = "abcde"): String = {
        val inputFile =
            new File("/home/ashubin/projects/scala/bitmap-cryptor/tmp/input.bmp")
        val outputFile =
            new File("/home/ashubin/projects/scala/bitmap-cryptor/tmp/output.bmp")
        outputFile.createNewFile

        val outputSource = new PrintWriter(outputFile, "ISO-8859-1")
        val dataSource = Source.fromFile(dataFile, "UTF-8").toArray
        val fileSource = Source.fromFile(inputFile, "ISO-8859-1").toArray

        val dataIterator = dataSource.iterator
        val fileIterator = fileSource.iterator

        // has side effect, it reads from fileIterator and writes to outputSource
        def writeByte(db: Int) {
            def iterate(fb: Int, s: Int) {
                outputSource.write((fb & 0xfc) + ((db >> s) & 0x03))
                if (fileIterator.hasNext && s > 0x00)
                    iterate(fileIterator.next, s - 0x02)
            }

            iterate(fileIterator.next, 0x06)
        }

        // write header info without changes
        var offset = getHeaderSize(fileSource)
        for (i <- 0 until offset if fileIterator.hasNext) outputSource.write(fileIterator.next)

        // write data until it empty
        while (dataIterator.hasNext) writeByte(dataIterator.next)

        // write end data
        if (fileIterator.hasNext) writeByte(-1)

        // write the rest file unchanged
        while (fileIterator.hasNext) outputSource.write(fileIterator.next)

        outputSource.flush
        outputSource.close

        outputFile.getName
    }

    def decrypt(fileName: String, secretKey: String = "abcde"): String = {
        val inputFile =
            new File("/home/ashubin/projects/scala/bitmap-cryptor/tmp/output.bmp")
        val outputFile =
            new File("/home/ashubin/projects/scala/bitmap-cryptor/tmp/message.txt")

        val inputSource = Source.fromFile(inputFile, "ISO-8859-1").toArray
        val outputSource = new PrintWriter(outputFile, "UTF-8")

        val inputIterator = inputSource.iterator

        val offset = getHeaderSize(inputSource)
        for (i <- 0 until offset if inputIterator.hasNext) inputIterator.next

        // has side effect, it reads from inputIterator
        def readByte(ib: Int, db: Int, s: Int): Int = {
            if (inputIterator.hasNext && s > 0x00)
                readByte(inputIterator.next, db | ((ib & 0x03) << s), s - 0x02)
            else db | ((ib & 0x03) << s)
        }

        var dataByte = 0x0
        while (inputIterator.hasNext && dataByte != 255) {
            dataByte = readByte(inputIterator.next, 0x0, 0x06)
            outputSource.write(dataByte)
        }

        outputSource.flush
        outputSource.close

        outputFile.getName
    }

    private def getHeaderSize(source: Array[Char]): Int =
        (10 until 14).map(i => { source(i) * math.pow(256, i - 10) }).reduceLeft(_ + _).toInt

}

// vim: set ts=4 sw=4 et:
