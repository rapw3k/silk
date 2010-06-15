package de.fuberlin.wiwiss.silk.hadoop

import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat
import java.io.DataOutputStream
import org.apache.hadoop.mapreduce.{TaskAttemptContext, RecordWriter}
import org.apache.hadoop.io.{Text, NullWritable}

class SilkOutputFormat extends FileOutputFormat[Text, InstanceSimilarity]
{
    override def getRecordWriter(job : TaskAttemptContext) : RecordWriter[Text, InstanceSimilarity] =
    {
        val conf = job.getConfiguration
        val file = getDefaultWorkFile(job, ".nt")
        val fs = file.getFileSystem(conf)
        val out = fs.create(file, false)

        new LinkWriter(out)
    }

    private class LinkWriter(out : DataOutputStream) extends RecordWriter[Text, InstanceSimilarity]
    {
        override def write(sourceUri : Text, instanceSimilarity : InstanceSimilarity) : Unit =
        {
            val line = sourceUri + " " + Silk.linkSpec.linkType + " " + instanceSimilarity.targetUri + ".\n"
            out.write(line.getBytes("UTF-8"))
        }

        override def close(context : TaskAttemptContext) : Unit =
        {
            out.close()
        }
    }
}
