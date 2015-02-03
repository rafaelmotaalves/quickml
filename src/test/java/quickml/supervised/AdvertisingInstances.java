package quickml.supervised;

import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import quickml.Benchmarks;
import quickml.utlities.CSVToInstanceReader;
import quickml.utlities.CSVToInstanceReaderBuilder;
import quickml.data.AttributesMap;
import quickml.data.Instance;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

/**
 * Created by alexanderhawk on 12/30/14.
 */
public class AdvertisingInstances {
    private static final Logger logger = LoggerFactory.getLogger(AdvertisingInstances.class);

    public static List<Instance<AttributesMap, Serializable>> getAdvertisingInstances(){
       CSVToInstanceReader csvToInstanceReader = new CSVToInstanceReaderBuilder().collumnNameForLabel("outcome").buildCsvReader();
       ArrayList<Instance<AttributesMap, Serializable>> advertisingInstances = Lists.newArrayList();

       try {
          final BufferedReader br = new BufferedReader(new InputStreamReader((new GZIPInputStream(Benchmarks.class.getResourceAsStream("advertisingData.csv.gz")))));
           advertisingInstances = csvToInstanceReader.readCsvFromReader(br);

       } catch (Exception e) {
           logger.error("failed to get advertising instances");
           throw new RuntimeException("failed to get advertising instances");

       }
       return advertisingInstances;
   }
}