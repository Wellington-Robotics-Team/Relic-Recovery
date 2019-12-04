package org.firstinspires.ftc.robotcontroller.internal;

import com.qualcomm.hardware.modernrobotics.ModernRoboticsI2cGyro;
import com.qualcomm.hardware.modernrobotics.ModernRoboticsI2cRangeSensor;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.hardware.ColorSensor;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.robotcontroller.external.samples.SensorMRGyro;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;

/**
 * Created by Sean on 11/2/2017.
 */
@Autonomous(name = "RedSideAutoTesting", group = "Auto")
public class RedSideAutoTesting extends  OpMode{

    private ElapsedTime runtime = new ElapsedTime();
    private Servo servo = null;
    private ColorSensor Jewelcc = null;
    private ModernRoboticsI2cRangeSensor range = null;
    private ModernRoboticsI2cGyro gyro = null;
    HardwareKraken Armbot;

    @Override
    public void init() {
        telemetry.addData("Status", "Initialized");

    }

    double position = 0;
    double power = 0.2;
    double Runtime = 0;

    Boolean Activemode = true;
    Boolean Jewelred = false;
    Boolean Jewelblue = false;
    Boolean Armdown = false;
    Boolean Potentialstop1 = false;
    Boolean Potentialstop2 = false;

    @Override
    public void start() {
        Armbot = new HardwareKraken();
        Armbot.init(hardwareMap);

        servo = hardwareMap.servo.get("JewelArm");
        Jewelcc = hardwareMap.colorSensor.get("Jewelcc");
        Jewelcc.enableLed(true);

        Armbot.Extender.setPosition(0.8);
        Armbot.ArmShoulder.setPosition(1);
        Armbot.ArmElbow.setPosition(0.95);
        gyro = hardwareMap.get(ModernRoboticsI2cGyro.class,"gyro");
        gyro.calibrate();
        while (gyro.isCalibrating()){
            telemetry.addData("status","gyro calibrating");
            telemetry.update();
        }

        range = hardwareMap.get(ModernRoboticsI2cRangeSensor.class,"range");

        runtime.reset();
    }
    double headingSigned = 0;
    double dontuseheading = 0;
    double Oldruntime = 0;
    double power2 = 0.3  ;
    @Override
    public void loop() {
        dontuseheading = gyro.getHeading();
        telemetry.addData("Status", "Running: ", runtime.toString());

        Runtime = runtime.time();
        //give the gyro code sign
        if(dontuseheading>180){
            headingSigned = dontuseheading-360;
        }
        else{
            headingSigned = dontuseheading;
        }
        telemetry.addData("heading",dontuseheading);
        telemetry.addData("heading signed",headingSigned);
        telemetry.update();

        double red = Jewelcc.red(); //This determines the red value of the jewel.
        double blue = Jewelcc.blue();
        double green = Jewelcc.green();
        telemetry.addData("Red Value", red);
        telemetry.addData("Blue Value", blue);
        telemetry.addData("Green Value", green);

        if (Runtime >= 0 && Runtime <= 0.3) { //This is the state that lowers the servo arm.
            position = 0.65;
            servo.setPosition(position);
            Armdown = true;
        }
        if (Armdown&&Runtime>=0.3&&!Jewelblue&&!Jewelred) { //This is the state where the sensor checks if the jewel is blue.
            if (blue > red && blue > green) {
                Jewelred = true;
                Oldruntime = runtime.time();
            }
        }

        if (Jewelblue) { //This is the state that runs when the jewel is blue.
            //The values used in this if condition assume that we are looking for the blue jewel.
            //This code will run if the jewel detected is blue.
            //If you want to change the program to hit the red jewel, change the values to >= 9 and <=11.

            if (runtime.time() <= Oldruntime+1) {
                Armbot.OmniDrive(power,0,0);
                Runtime = runtime.time();
            }
            else {

                //This is what will stop the motors from continuing endlessly.
                power = 0;
                //Armbot.OmniDrive(0,0,0);
                //Jewelcc.enableLed(false); //This puts the color sensor in passive mode.
                position = 0;
                servo.setPosition(position); //This returns the servo to its original position.
                Armdown = false;
            }
        }

        if (!Jewelblue && Armdown && Runtime>=0.3 && !Jewelred) { //This is where the color sensor checks to see if the jewel is red.
            Potentialstop1 = true;
            if (red > blue && red > green) {
                Jewelblue = true;
                Oldruntime = runtime.time();
            }
        }

        if (Jewelred) {//This runs when the jewel is red.
            //The values used in this if condition assume that we are looking for the blue jewel.
            //This code will run if the jewel detected is red.
            //If you want to change the program to hit the red jewel, change the values to >= 2 and <=4.

            if (runtime.time() <= Oldruntime+1) {
                Armbot.OmniDrive(-power,0,0);
            }
            else{
                power = 0;
                Armbot.OmniDrive(power,0,0);
                // Jewelcc.enableLed(false);
                position = 0;
                servo.setPosition(position);
                Armdown = false;
            }
        }

        if (Jewelred == false && Armdown&&Runtime>4) { //This is one of the things that will cause the arm to raise.
            Potentialstop2 = true;

        }

        if (Potentialstop1 && Potentialstop2) { //This is what returns everything to normal.
            //This is the safety code that runs if the jewel has moved out of range.
            //This allows the program to safely stop the motors without the use of the color sensor.
            Runtime = runtime.time();
            power = 0;
            //Armbot.OmniDrive(power,0,0);

            if(runtime.time() <= Runtime + 1) {
                //Armbot.OmniDrive(power,0,0 );
            }
            else {
                Jewelcc.enableLed(false);
                position = 0;
                servo.setPosition(position);
            }
        }
        if(runtime.time()>Oldruntime+2){
            //colorsensor is on the right side
            //back up
            Jewelcc.enableLed(false);
            position = 0;
            servo.setPosition(position);
            //if(headingSigned)
            if(range.getDistance(DistanceUnit.INCH)<=10){
                power2 = 0;
            }

            Armbot.OmniDrive(power2,0,0);//backwards
        }




    }

    /*
     * Code to run ONCE after the driver hits STOP
     */
    @Override
    public void stop() {
        //LeftFrontM.setPower(0);
        //LeftBackM.setPower(0);
        //RightFrontM.setPower(0);
        //RightBackM.setPower(0);
    }
}


