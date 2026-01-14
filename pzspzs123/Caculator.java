import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

//显示面板
class Calcpane extends VBox {
    static TextField textField = new TextField();
    //存储运算符
    static TextField st = new TextField();
    //小按钮结果存储
    static TextField mt = new TextField("0");
    public Calcpane(){
        setSpacing(10);//间距
        textField.setAlignment(javafx.geometry.Pos.CENTER);
        textField.setLayoutX(40);//文本框位置
        textField.setLayoutY(80);
        // 设置文本框的大小
        textField.setMinSize(450, 80); // 设置最小宽度和高度
        textField.setMaxSize(500, 80); // 设置最大宽度和高度

        // 设置文本框的文字大小
        textField.setStyle("-fx-font-size: 20px;");
        getChildren().addAll(textField, new Cpane(),new BTpane());

    }

}

//计算器按钮
class ButtonC extends Button{
    //x控制运算符，w控制是否yz转换，z为前一个值，y为当前值,qa保证=后数字能换为负号
    static double x=-1, y, z,w=0,qa=0;
    //保证文本框上现在为数字
    static double rr=0;
    ButtonC(String s){
        super(s);
        setFont(javafx.scene.text.Font.font(30));
        setPrefSize(100,60);
        setOnAction(e -> {
            String b = ((ButtonC) e.getSource()).getText();
            if(ButtonS.mr==1){
                return;
            }

            //判断b第一位是否为数字
            if( Character.isDigit(b.charAt(0))||b.equals(".")){
                if(x==1&&w==0){
                    Calcpane.textField.setText("");
                    z=y;
                    w++;
                }
                //每次添加文本于原文本
                rr=1;
                Calcpane.textField.setText(Calcpane.textField.getText()+b);
                if(qa==1){
                        qa=0;
                        Calcpane.textField.setText("");
                        Calcpane.st.setText("");
                        x=-1;
                        w=0;
                    }
            }
            else if(b.equals("AC")){
                Calcpane.textField.setText("");
                Calcpane.st.setText("");
                x=-1;
                w=0;
                qa = 0;
            }
            else if(b.equals("+/-")&&rr==1){
                double ar = Double.parseDouble(Calcpane.textField.getText());
                Calcpane.textField.setText(String.valueOf(ar*-1));
                if(qa==1){
                    qa=0;
                    y=-y;
                }
            }
            else if(b.equals("%")&&rr==1){
                double sr = Double.parseDouble(Calcpane.textField.getText());
                Calcpane.textField.setText(String.valueOf(sr/100));
                if(qa==1){
                    y=y/100;
                }
            }
            else{
                if(b.equals("<-")){
                    String p = Calcpane.textField.getText();
                    if(p.equals("+")||p.equals("-")||p.equals("×")||p.equals("÷")){
                        x=0;
                    }
                    Calcpane.textField.setText(Calcpane.textField.getText().substring(0,Calcpane.textField.getText().length()-1));
                    if(qa==1){
                        qa=0;
                        Calcpane.textField.setText("");
                        Calcpane.st.setText("");
                        x=-1;
                        w=0;
                    }

                }
                else if((b.equals("+")||b.equals("-")||b.equals("×")||b.equals("÷"))&&x!=1){
                    rr=0;
                    if(x==-1) {
                        y = Double.parseDouble(Calcpane.textField.getText());
                    }
                    x = 1;
                    Calcpane.textField.setText(b);
                    Calcpane.st.setText(b);
                }
                else if(b.equals("=")&&w==1){
                    String t = Calcpane.st.getText();
                    y = Double.parseDouble(Calcpane.textField.getText());
                    switch (t){
                        case "+":
                            Calcpane.textField.setText(String.valueOf(z+y));
                            y=z+y;
                            break;
                        case "-":
                            Calcpane.textField.setText(String.valueOf(z-y));
                            y=z-y;
                            break;
                        case "×":
                            Calcpane.textField.setText(String.valueOf(z*y));
                            y=z*y;
                            break;
                        case "÷":
                            Calcpane.textField.setText(String.valueOf(z/y));
                            y=z/y;
                    }
                    w=0;
                    x=0;
                    qa=1;
                }
                }


        });
    }
}

//小按钮
class ButtonS extends Button{
    static int mr=0;
    static String mrs;
    //存储数据
    static double mc;
    ButtonS(String s){
        super(s);
        setFont(javafx.scene.text.Font.font(20));//设置字体大小
        setPrefSize(65,30);//设置大小
        setOnAction(e -> {
            String y = ((ButtonS) e.getSource()).getText();
            if(y.equals("MR")&&(ButtonC.rr==1)){
                if(mr==0) {
                    mrs = Calcpane.textField.getText();
                    Calcpane.textField.setText(mrs + "   存储的数据：" + Calcpane.mt.getText());
                    mr=1;
                }
                else if(mr==1){
                    Calcpane.textField.setText(mrs);
                    mr=0;
                }
            }
            else {
                if(mr==1){
                    return;
                }
                if (y.equals("MC") && (ButtonC.rr == 1)) {
                    Calcpane.mt.setText("");
                } else if (y.equals("M+") && (ButtonC.rr == 1)) {
                    mc = Double.parseDouble(Calcpane.textField.getText()) + Double.parseDouble(Calcpane.mt.getText());
                    Calcpane.mt.setText(String.valueOf(mc));
                } else if (y.equals("M-") && (ButtonC.rr == 1)) {
                    mc = Double.parseDouble(Calcpane.mt.getText()) - Double.parseDouble(Calcpane.textField.getText());
                    Calcpane.mt.setText(String.valueOf(mc));
                } else if (y.equals("MS") && (ButtonC.rr == 1)) {
                    mc = Double.parseDouble(Calcpane.textField.getText());
                    Calcpane.mt.setText(String.valueOf(mc));
                } else if (y.equals("M~") && (ButtonC.rr == 1)) {
                    mc = Double.parseDouble(Calcpane.textField.getText());
                    Calcpane.textField.setText(Calcpane.mt.getText());
                    Calcpane.mt.setText(String.valueOf(mc));
                }
            }
        });
    }
}


//小按钮面板
class Cpane extends GridPane{
    public Cpane(){
        setPadding(new Insets(40,0,0,0));//间距
        setPrefSize(300,10);
        setHgap(7);//横向间距
        addRow(0, new ButtonS("MC"), new ButtonS("MR"), new ButtonS("M+"),
        new ButtonS("M-"), new ButtonS("MS"),new ButtonS("M~") );
    }
}

//按钮面板
class BTpane extends GridPane{
    public BTpane(){
        setPadding(new Insets(0,0,0,0));//间距
        setPrefSize(300,400);
        setVgap(15);//纵向间距
        setHgap(10);//横向间距
        addRow(1, new ButtonC("AC"), new ButtonC("%"), new ButtonC("<-"), new ButtonC("÷"));
        addRow(2, new ButtonC("7"), new ButtonC("8"), new ButtonC("9"), new ButtonC("×"));
        addRow(3, new ButtonC("4"), new ButtonC("5"), new ButtonC("6"), new ButtonC("-"));
        addRow(4, new ButtonC("1"),new ButtonC("2"), new ButtonC("3"),  new ButtonC("+"));
        addRow(5, new ButtonC("+/-"),new ButtonC("0"), new ButtonC("."), new ButtonC("="));

    }
}




public class Caculator extends Application {
    public static void main(String[] args) {
        launch(args);

    }
    @Override
    public void start(Stage primaryStage) throws Exception {
        Scene scene = new Scene(new Calcpane(), 430, 600);
        primaryStage.setScene(scene);
        primaryStage.setTitle("计算器");
        primaryStage.show();

    }
}
