package ai.mcts.huct;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

public class ExperimentData {

    public String player0;
    public String player1;
    public int lookaheadTime;
    public List<Integer> results;
    public  List<Integer> times;
    public  List<Integer> timeForSimulations;
    public  List<Integer> savedTrees;
    public String path;
    public ExperimentData(String name,String player0, String player1,int lookaheadTime){
        results = new ArrayList<>();
        times = new ArrayList<>();
        timeForSimulations = new ArrayList<>();
        savedTrees = new ArrayList<>();
        this.path = name+"-"+ player0 +"-"+ player1 +"-"+ lookaheadTime +".csv";
    }

    public String toString(){
        String s = "path ="+"\n";
        StringBuilder sb= new StringBuilder(s);
        for(int i=0;i<results.size();i++){
            sb.append(i+"");
            sb.append(",");
            sb.append(results.get(i).toString());
            sb.append(",");
            sb.append(times.get(i).toString());
            /*sb.append(",");
            sb.append(timeForSimulations.get(i).toString());
            sb.append(",");
            sb.append(savedTrees.get(i).toString());*/
            sb.append("\n");
        }
        return sb.toString();
    }

    public String toFile(){

        try {
            FileWriter fw = new FileWriter(path);

            fw.append("Index");
            fw.append(",");
            fw.append("Result");
            fw.append(",");
            fw.append("Time");
           /* fw.append(",");
            fw.append("DurationPerMatch");
            fw.append(",");
            fw.append("SavedTree");*/
            fw.append("\n");

            if(!results.isEmpty()
                    && results.size() != times.size()
                    /*&& results.size() != timeForSimulations.size()
                    && results.size() !=savedTrees.size()*/
                    )
                System.out.println("anomaly");

            for(int i=0;i<results.size();i++){
                fw.append(i+"");
                fw.append(",");
                fw.append(results.get(i).toString());
                fw.append(",");
                fw.append(times.get(i).toString());
                /*fw.append(",");
                fw.append(timeForSimulations.get(i).toString());
                fw.append(",");
                fw.append(savedTrees.get(i).toString());*/
                fw.append("\n");
            }

            fw.close();
        }catch (Exception exc){

        }
        return path;
    }

    public static ExperimentData FromFile(String rawPath){

        String raw =  rawPath.substring(0,rawPath.length() -4);
        String[] temps = raw.split("-");
        String path= temps[0];
        String player0 = temps[1];
        String player1 = temps[2];
        int lookaheadTime = Integer.parseInt(temps[3]) ;
        ExperimentData data = new ExperimentData(path,player0,player1,lookaheadTime);

        boolean isFirstTime = false;
        try{
            try (BufferedReader br = new BufferedReader(new FileReader(rawPath))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if(!isFirstTime){
                        isFirstTime= true;
                        continue;
                    }
                    // process the line.
                    String[] props = line.split(",");

                    data.results.add(Integer.parseInt(props[1]));
                    data.times.add(Integer.parseInt(props[2]));
                    //data.timeForSimulations.add(Integer.parseInt(props[2]));
                    //data.savedTrees.add(Integer.parseInt(props[3]));
                }
            }
        }catch (Exception exc){

        }
        return  data;
    }
}
