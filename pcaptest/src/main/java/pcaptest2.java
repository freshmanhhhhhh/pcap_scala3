import java.io.*;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.ObjectWritable;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.function.MapFunction;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.hadoop.mapred.SequenceFileInputFormat;
import scala.Tuple2;

import static scala.math.BigDecimal.binary;

//import org.apache.hadoop.mapreduce.lib.input.SequenceFileAsBinaryInputFormat;
public class pcaptest2 {
    public static byte[] toByteArray(Object obj) {
        byte[] bytes = null;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(obj);
            oos.flush();
            bytes = bos.toByteArray ();
            oos.close();
            bos.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return bytes;
    }
    public static byte[] ObjectToByte(Object obj) {
        byte[] bytes = null;
        try {
            // object to bytearray
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            ObjectOutputStream oo = new ObjectOutputStream(bo);
            oo.writeObject(obj);

            bytes = bo.toByteArray();

            bo.close();
            oo.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bytes;
    }
    public static void main(String args[]) throws IOException {
        String warehouseLocation = new File("spark-warehouse").getAbsolutePath();
        SparkSession spark = SparkSession
                .builder()
                .appName("Java Spark Hive Example")
                .master("local[*]")
                .config("spark.sql.warehouse.dir", warehouseLocation)
                .enableHiveSupport()
                .getOrCreate();
        //spark.sql("CREATE TABLE IF NOT EXISTS src (TIMESTAMP long, TIMESTAMP_USEC long,TIMESTAMP_MICROS long) USING hive OPTIONS(fileFormat 'org.apache.hadoop.mapred.SequenceFileAsBinaryInputFormat',outputFormat 'org.apache.hadoop.hive.ql.io.HiveIgnoreKeyTextOutputFormat',serde 'MySerDe')");
        //spark.sql("CREATE TABLE IF NOT EXISTS src (ts bigint,ts_micros bigint,ttl int,ip_version int,ip_header_length int) USING hive OPTIONS(fileFormat 'sequencefile',serde 'PcapDeserializer')");
        spark.sql("CREATE TABLE IF NOT EXISTS src (ts bigint, ts_usec double, protocol string, src string, src_port int, dst string, dst_port int, len int, ttl int, dns_queryid int, dns_flags string, dns_opcode string, dns_rcode string, dns_question string, dns_answer array<string>, dns_authority array<string>, dns_additional array<string>,pcapByte binary) USING hive OPTIONS(fileFormat 'sequencefile',serde 'PcapDeserializer')");
        spark.sql("LOAD DATA LOCAL INPATH '/home/bjbhaha/Envroment/hadoop-2.7.3/bin/music31.seq' INTO TABLE src").show();

// Queries are expressed in HiveQL
        JavaRDD<Object> pcapByte= spark.sql(args[0]).toJavaRDD().map(row->row.get(0));

        //pcapByte.foreach(x->System.out.println(new BytesWritable((byte[])((byte[])(x)))));
        DataOutputStream dos = new DataOutputStream(new FileOutputStream("/home/bjbhaha/Desktop/music31.pcap"));
        byte pcapHeader[] = new byte[]{(byte) 0xD4, (byte) 0xC3, (byte) 0xB2, (byte) 0xA1, 0x02, 0x00, 0x04,
                0x00,0x00, 0x00, 0x00, 0x00, 0x00, 0x00,0x00,0x00,0x00,0x00,0x04,0x00,0x01,0x00,0x00,0x00};
        dos.write(pcapHeader,0,24);
        List<byte[]> list=new ArrayList<>();
//        JavaRDD<BytesWritable> pcapByte2=pcapByte.map(x->{
//            long packetSize = PcapReaderUtil.convertInt((byte[])((byte[])(x)), 8, false);
//            BytesWritable a= new BytesWritable((byte[])((byte[])(x)));
//            a.setSize((int)(packetSize)+16);
//            return a;
//        });

        list=pcapByte.map(x->{//
            long packetSize = PcapReaderUtil.convertInt((byte[])((byte[])(x)), 8, false);
            int l=(int)packetSize+16;
            byte[] a=new byte[l];
            System.arraycopy((byte[])((byte[])(x)),0,a,0,l);
            return a;
        }).collect();
        list.forEach(tt->{
            System.out.println(new BytesWritable(tt));
            try {
                dos.write(tt, 0, tt.length);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        //spark.sql("SELECT * FROM src").show();
    }
}
