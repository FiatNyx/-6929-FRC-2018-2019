/*----------------------------------------------------------------------------*/
/* 	      Code du betabot de robotique cuivre et or 2018                  */
/*  Comme nous supportons le professionalisme coopératif, vous pouvez vous    */
/*  		  inspirer de ce code sans aucune gene.			      */
/*                                                                            */
/* 	    (C'est toutefois bien mieux si vous crez le votre,                */
/*		   il n'y a pas mieux pour se pratiquer                       */
/*----------------------------------------------------------------------------*/
//Ps. oui il n'y a pas d'accent. Meme dans les commentaires, ca fait planter le gradle

package org.usfirst.frc.team6929.robot;

//Importations de code des differentes librairies

import com.kauailabs.navx.frc.AHRS;
import edu.wpi.cscore.UsbCamera;
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.wpilibj.Joystick;
import edu.wpi.first.wpilibj.PIDController;
import edu.wpi.first.wpilibj.SPI;
import edu.wpi.first.wpilibj.Spark;
import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.VictorSP;
import edu.wpi.first.wpilibj.drive.DifferentialDrive;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj.CameraServer;
//import edu.wpi.first.wpilibj.Compressor;
import edu.wpi.first.wpilibj.DigitalInput;
import edu.wpi.first.wpilibj.DoubleSolenoid;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.Encoder;

//-----------------Fin des importations et debut du code!--------------------------------

//On annonce la classe. C'est une classe TimedRobot, 
//ce qui signifie qu'elle va etre appelee (Utilisee toutes les 20 ms (Constant))

public class Robot extends TimedRobot {
//On annonce les differentes variables des pieces du robot
	
	VictorSP leftMotor = new VictorSP(0);
	VictorSP rightMotor = new VictorSP(1);
	Spark MPR1 = new Spark(2);
	Spark MPC1 = new Spark(3);
	Spark MPC2 = new Spark(4);
	Spark MPelle1 = new Spark(5);
	
	private DifferentialDrive m_robotDrive = new DifferentialDrive(leftMotor, rightMotor);
	private Joystick m_stick = new Joystick(0);
	private Joystick coPilote_stick = new Joystick(1);
	
	private Timer limit_timer = new Timer();
	private Timer yButtonTimer = new Timer();
	private Timer PID_timer = new Timer();
	private Timer retourTimer = new Timer();
	private Timer pelleTimer = new Timer();
	
	AHRS ahrs;
   	DigitalInput limitswitchPince;
	
	Encoder encGauche;
    	Encoder encDroit;
     	Encoder encPelle;
     	Encoder encPince;
     
	
//Chooser
	//Ce sont les variables permettant de choisir le mode autonome avant le match
	//Elles sont un peu speciales a cause de cela
	
		private static final String kDefaultAuto = "Mode default";
		private static final String kCustomAuto1 = "Mode 1";
		private static final String kCustomAuto2 = "Mode 2";
		private static final String kCustomAuto3 = "Mode 3";
		private static final String kCustomAuto4 = "Mode 4";
		private static final String kCustomAuto5 = "Mode 5";
		private static final String kCustomAuto6 = "Mode 6";
		private String m_autoSelected;
		private SendableChooser<String> m_chooser = new SendableChooser<>();
	
	
//PID
	//Les variables pour le regulateur PID (proportionnel, integral, derive)
	
	//Des constantes de PID qui marchent bien avec le robot

        private static final double kP = -0.04; // -0.035
		private static final double kI = 0;    
		private static final double kD = -0.065; //-0.065
		
	//On annonce le controlleur PID
	
		private PIDController r_pidController;
		private PIDController l_pidController;

		private PIDController encPince_pidController;
	
	//Les variables du pneumatic
	
		//DoubleSolenoid exampleDouble = new DoubleSolenoid(0, 7);
		DoubleSolenoid pneuPince = new DoubleSolenoid(4, 5);
		DoubleSolenoid pneuPelle = new DoubleSolenoid(6, 7);
		//DoubleSolenoid pneuPelleL = new DoubleSolenoid(1, 6);
		
	//Ah ce tres cher Network Table, utilise pour GRIP
	
		NetworkTableEntry xCenterEntry;
	 	double[] centerX;
	 
	 
	//Variables personnalisees (Qui ne sont que des variables fictives et non pas des elements du robot)
	
	boolean Bbuton = false;
	boolean Ybuton = false;
	boolean lent = true;
	boolean lentTrigger = false;
	boolean YbutonTrigger = false;
	boolean premiereMesure = false;
	boolean autoLock1 = false;
	boolean autoLock2 = false;
	boolean autoPince = false;
	
	int ordreAuto = 0;
	double calcul = 0;
	double objRetour = 0;
	double objectifStraight = 0;
	boolean yButonAngle = false;
	boolean retourAuto = false;
	int rotationPelle=1;
	boolean pelleAvance = false;
	boolean GripOk = false;
	
	boolean rotationPelle90Plus = false;
	boolean rotationPelle90Moins = false;
	boolean rotationPelle180Plus = false;
	boolean rotationPelle180Moins = false;
	boolean pelleHaut = false;
	
//Fin de l'annonce des variables

//-----------------------Lancement du robot------------------------------------------
	@Override
	public void robotInit() {
	      try {
	          ahrs = new AHRS(SPI.Port.kMXP); 
	      } catch (RuntimeException ex) {
	          DriverStation.reportError("Error instantiating navX-MXP:  " + ex.getMessage(), true);
	      }
	
//On setup certaines pieces des le demarage
	      	ahrs.reset();
		
		UsbCamera camera = CameraServer.getInstance().startAutomaticCapture();
		camera.setResolution(640, 360);
		camera.setFPS(15);
		camera.setExposureManual(30);

		limitswitchPince = new DigitalInput(6);

		
		//Encoders

		encDroit = new Encoder(0, 1, false, Encoder.EncodingType.k1X);
		encGauche = new Encoder (2,3, false, Encoder.EncodingType.k1X);
		encPince = new Encoder(4,5,false, Encoder.EncodingType.k1X);
		//enc1.setDistancePerPulse(360/497);
		
//Les trois versions du code de pneumatic qu'on peut utiliser, a se rappeler
		//exampleDouble.set(DoubleSolenoid.Value.kOff);
		//exampleDouble.set(DoubleSolenoid.Value.kForward);
		//exampleDouble.set(DoubleSolenoid.Value.kReverse);
		
		
//Network Tables
	//On commence a collecter les donnees du network
	
		NetworkTableInstance inst = NetworkTableInstance.getDefault();
		NetworkTable table = inst.getTable("GRIP/Contours");
		xCenterEntry = table.getEntry("centerX");
		
//Selection mode auto
	//On ajoute des options de mode autonomes
	
			m_chooser.addDefault("Mode default", kDefaultAuto);
			m_chooser.addObject("Mode A et B - Gauche", kCustomAuto1);
			m_chooser.addObject("Mode A et B - Droite", kCustomAuto2);
			m_chooser.addObject("Mode C - Gauche", kCustomAuto3);
			m_chooser.addObject("Mode C - Droite", kCustomAuto4);
			m_chooser.addObject("Mode D - Gauche", kCustomAuto5);
			m_chooser.addObject("Mode D - Droite", kCustomAuto6);
	
	//Et on les mets sur le dashboard		
	
			SmartDashboard.putData(m_chooser);
		    
//PID
	//On active le PID
	
		r_pidController = new PIDController(kP, kI, kD,  ahrs, leftMotor);
		l_pidController = new PIDController(kP, kI, kD, ahrs, rightMotor);
		
		
		
		r_pidController.setInputRange(-180, 180);
		l_pidController.setInputRange(-180, 180);
		r_pidController.setContinuous();
		l_pidController.setContinuous();
		
		
		 encPince_pidController = new PIDController(0.175, 0 , 0 , encPince, MPR1);
		 encPince_pidController.setOutputRange(0, 0.75);

//Fin du robot init	
	}

//-----------------------------On start le mode autonome------------------------- 
	@Override
	public void autonomousInit() {
	//Ont active ou reinitialise certains systemes
		encPince.reset();
		encGauche.reset();
		encDroit.reset();
		
		limit_timer.reset();
		retourTimer.reset();
		PID_timer.reset();
		
		
		
		r_pidController.disable();
		l_pidController.disable();
		
		m_autoSelected = m_chooser.getSelected();
		System.out.println("Auto selected: " + m_autoSelected);
		ordreAuto = 0;
		
		autoLock1 = false;
		autoLock2 = true;
		retourAuto = false;
		premiereMesure = false;
		
		ahrs.reset();
		ahrs.resetDisplacement();
//Fin de autonomousInit
	}


//--------------------Mode autonome (Durant)----------------------------------
//--------------------En chantier---------------------------------------------

	@Override
	public void autonomousPeriodic() {
	//on lit les differents outilis de mesure sur le robot
	//Ici, c'est les encodeurs sur les roues	
		
			double countGauche = encGauche.get();
			double countDroit = encDroit.get();
			double roueGauche = -countGauche/240;
			double roueDroit = -countDroit/240;
			double rouefalse = roueGauche - roueDroit;
			double roue = rouefalse / 2;
		
	//On verifie le mode autonome choisie sur le smartDashboard
	//Dependament du mode choisis, le parcours, et donc les etapes, sera different
	//Ainsi, il faut tout changer pour chaque mode autonome
	//Le parcours est pre-defini, avec une partie en reconnaissance visuelle afin de s'assurer que
	//le robot appercoive et prenne la carrote (Le cylindre qu'il faut rammener)
		
		switch (m_autoSelected) {
		case kCustomAuto1:
			 SmartDashboard.putNumber("Ok",1);
			 
				//--------------------------Variables auto 1-----------------------------------

								
									 countGauche = encGauche.get();
									 countDroit = encDroit.get();
									 roueGauche = -countGauche/240;
									roueDroit = -countDroit/240;
									 rouefalse = roueGauche - roueDroit;
									 roue = rouefalse / 2;
							//480 = un tour
									SmartDashboard.putNumber("Tours", roue);
							
							
				//----------------------Debut mode autonome 1--------------------------
							//roue  < 2.5
							if(retourAuto == false && ordreAuto == 0) {
								
								double turningValue = -ahrs.getAngle();
								SmartDashboard.putNumber("Turning Value", turningValue);

								if(turningValue > 0.3) {
									turningValue = 0.3;
								}
								
								else if (turningValue < -0.3) {
									turningValue = -0.3;
								}

								m_robotDrive.arcadeDrive(0.65, -turningValue * 0.7);
								
									if(roue >= 2.2) {
										ordreAuto = 1;
									}
									
							//PID_timer.get() < 0.8
							}else if (retourAuto == false && ordreAuto == 1 ){
								if(autoLock1 == false) {
									//PID_timer.start();
									ahrs.reset();
									encGauche.reset();
									encDroit.reset();
									PID_timer.start();
									r_pidController.enable();
									l_pidController.enable();
									r_pidController.setSetpoint(40); //48
									l_pidController.setSetpoint(40);
									r_pidController.setOutputRange(-0.7, 0.7); 
									l_pidController.setOutputRange(-0.7, 0.7); 
									r_pidController.setAbsoluteTolerance(0.001);
									l_pidController.setAbsoluteTolerance(0.001);
									autoLock1 = true;
									autoLock2 = false;
								}
								SmartDashboard.putNumber("PId gyro value", ahrs.getAngle());
								if(r_pidController.onTarget() == true && l_pidController.onTarget() == true) {
									ordreAuto = 2;
									
								}else if(PID_timer.get() >= 1.2) {
									ordreAuto = 2;
									PID_timer.stop();
									PID_timer.reset();
								}
								
							//PID_timer.get() >= 0.8 && roue < 4.75
							}else if (retourAuto == false && ordreAuto == 2) {
								if(autoLock2 == false) {
								encGauche.reset();
								encDroit.reset();
									ahrs.reset();
									PID_timer.stop();
								
									r_pidController.disable();
									l_pidController.disable();
									
									pneuPince.set(DoubleSolenoid.Value.kForward);
									encPince_pidController.enable();
									encPince_pidController.setSetpoint(700);
								
									
									autoLock2 = true;
									autoLock1 = false;
								}
								
								double turningValue = -ahrs.getAngle() ;
								SmartDashboard.putNumber("Turning Value", turningValue);
								
								if(turningValue > 0.5) {
									turningValue = 0.5;
								}
								if (turningValue < -0.5) {
									turningValue = -0.5;
								}
								
								m_robotDrive.arcadeDrive(0.75, -turningValue * 0.7);
								
								if(roue >= 6.5) {
									ordreAuto = 3;
								}
								//PID_timer.get() < 1
							}else if ( retourAuto == false && ordreAuto == 3){
								if(autoLock1 == false) {
									PID_timer.reset();
									PID_timer.start();
									ahrs.reset();
									
									limit_timer.start();
									r_pidController.enable();
									l_pidController.enable();
									r_pidController.setAbsoluteTolerance(0.0001);
									l_pidController.setAbsoluteTolerance(0.0001);
									r_pidController.setSetpoint(90);
									l_pidController.setSetpoint(90);
									
									autoLock1 = true;
									autoLock2 = false;
									

								}
								if(PID_timer.get() >= 1.21) {
									ordreAuto = 4;
								}
								SmartDashboard.putNumber("Pid Timer",PID_timer.get());
								if(r_pidController.onTarget() == true && l_pidController.onTarget() == true) 
								{
									ordreAuto = 4;
								}
								//limit_timer.get() <=1.2 && PID_timer.get() >= 1
							}else if (retourAuto == false  && ordreAuto == 4){
								if(autoLock2 == false) {
								
									ahrs.reset();
									PID_timer.stop();
									r_pidController.disable();
									l_pidController.disable();
									SmartDashboard.putString("PidDisabled","Yes");
									
									autoLock2 = true;
									autoLock1 = false;
							}
								
								double turningValue = -ahrs.getAngle() ;
								SmartDashboard.putNumber("Turning Value", turningValue);
								
								if(turningValue > 0.5) {
									turningValue = 0.5;
								}
								if (turningValue < -0.5) {
									turningValue = -0.5;
								}
							
							//	m_robotDrive.arcadeDrive(0, -turningValue * 0.8);
								
								if(0 == 0) {
									ordreAuto = 5;
								}
								//roue  <= 10.5 
							}else if (ordreAuto == 5 || retourAuto == true ){
								
								retourAuto = true;
						
						if(limitswitchPince.get() == true && autoPince == false) {
									
									double[] gripValuesX = new double[0];
									double[] XValue =  xCenterEntry.getDoubleArray(gripValuesX);
									double angleVise = 320 * 7 / 147; // /10.33

								
									
									
									MPC1.set(0.45);
								MPC2.set(-0.45);
									
									if(XValue.length != 0) {

											if(premiereMesure == false) {
												encGauche.reset();
												encDroit.reset(); // /20.102
												ahrs.reset();
												double trouve =  XValue[0] * 7 / 147; //10.33 //7/127
												SmartDashboard.putNumber("Trouve", trouve);
												

												
										
												premiereMesure = true;  
												
												calcul = trouve - angleVise;
												SmartDashboard.putNumber("Calculs", calcul);
											}
											
											if(premiereMesure == true) {
												
												double angleGyro = ahrs.getAngle();
												SmartDashboard.putNumber("Angle Gyro", angleGyro);
												double setPoint =  calcul - angleGyro ;
												SmartDashboard.putNumber("Set Point", setPoint);
												
												double setPointTrue = -setPoint * 0.8;
												if(setPointTrue > 0.5) {
													setPointTrue = 0.5;
												}
												if (setPointTrue < -0.5) {
													setPointTrue = -0.5;
												}
												m_robotDrive.arcadeDrive(-0.575, setPointTrue); //0.025
											}
				                                                                                         
									}else if (premiereMesure == true) {
										double angleGyro = ahrs.getAngle();
										SmartDashboard.putNumber("Angle Gyro v2", angleGyro);
										double setPoint =  calcul - angleGyro ;
										SmartDashboard.putNumber("Set Point v2", setPoint);
										double setPointTrue = -setPoint * 0.8;
										if(setPointTrue > 0.5) {
											setPointTrue = 0.5;
										}
										if (setPointTrue < -0.5) {
											setPointTrue = -0.5;
										}
										m_robotDrive.arcadeDrive(-0.6,	setPointTrue); //0.025
							
									}else {
										m_robotDrive.arcadeDrive(0, 0.4);
								}
									//roue < 0.3
								}else if( ordreAuto == 5) {
									MPC1.set(0.15);
									MPC2.set(-0.15);
									autoPince = true;

					 			
									double turningValue = ahrs.getAngle();
								
									if(turningValue > 0.5) {
										turningValue = 0.5;
									}
									if (turningValue < -0.5) {
										turningValue = -0.5;
									}
					 			
									m_robotDrive.arcadeDrive(0.65, turningValue );
					 			
									if(roue >= -0.18) {
										ordreAuto = 6;
									}
									//retourTimer.get() < 1
								} else if (ordreAuto == 6) {
									MPC1.set(0.55);
									MPC2.set(-0.55);
									if(autoLock1 == false) {
										ahrs.reset();
										PID_timer.reset();
										PID_timer.start();
										
										
										retourTimer.start();
										r_pidController.enable();
										l_pidController.enable();
										r_pidController.setSetpoint(-88);
										l_pidController.setSetpoint(-88);
										
										r_pidController.setAbsoluteTolerance(0.001);
										l_pidController.setAbsoluteTolerance(0.001);
										autoLock1 = true;
										autoLock2 = false;
									}
									
									if(r_pidController.onTarget() == true && l_pidController.onTarget() == true) {
										ordreAuto = 7;
									}
									if(PID_timer.get() >= 1.2) {
										ordreAuto = 7;
									}
									
								//roue > -5.5 
					 		} else if (ordreAuto == 7) {
					 			MPC1.set(0.15);
					 			MPC2.set(-0.15);
					 			r_pidController.disable();
					 			l_pidController.disable();
								double turningValue = -ahrs.getAngle();
								SmartDashboard.putNumber("Turning Value", turningValue);
								
								if(turningValue > 0.3) {
									turningValue = 0.3;
								}
								if (turningValue < -0.3) {
									turningValue = -0.3;
								}
								m_robotDrive.arcadeDrive(-0.7, 0);
								
								if(roue <= -6) {
									ordreAuto = 8;
								}
								//retourTimer.get() < 3.5
					 		} else if (ordreAuto == 8) {

					 			MPC1.set(0.15);
					 			MPC2.set(-0.15);
					 			if(autoLock2 == false) {
					 				ahrs.reset();
						 			PID_timer.reset();
						 			PID_timer.start();
					 				r_pidController.enable();
					 				l_pidController.enable();
					 				r_pidController.setSetpoint(-50);
					 				l_pidController.setSetpoint(-50);
					 				
					 				autoLock2 = true;
					 				autoLock1 = false;
					 			}
					 			
					 			if(r_pidController.onTarget() == true && l_pidController.onTarget() == true) {
					 				ordreAuto = 9;
					 			}
					 			
					 			if(PID_timer.get() >= 0.9) {
					 				ordreAuto = 9;
					 			}
					 		} else if (ordreAuto == 9) {
					 			if(autoLock1 == false) {
					 				retourTimer.reset();
					 				retourTimer.start();
					 				
					 				autoLock1 = true;
					 				autoLock2 = false;
					 			}
					 			MPC1.set(0.15);
					 			MPC2.set(-0.15);
					 			r_pidController.disable();
					 			l_pidController.disable();
								double turningValue = -ahrs.getAngle();
								SmartDashboard.putNumber("Turning Value", turningValue);
								
								if(turningValue > 0.3) {
									turningValue = 0.3;
								}
								if (turningValue < -0.3) {
									turningValue = -0.3;
								}
								m_robotDrive.arcadeDrive(-0.6, 0);
								
								if(retourTimer.get() >= 0.32) {
									ordreAuto = 10;
								}
					 		}else if (ordreAuto == 10) {
								MPC1.set(-0.75);
								MPC2.set(0.75);
								
								if(retourTimer.get() >= 0.5) {
									ordreAuto = 11;
								}
					 		}else if(ordreAuto == 11) {
					 			if(autoLock2 == false) {
					 				ahrs.reset();
					 				retourTimer.reset();
					 				retourTimer.start();
					 				

					 				
						 			PID_timer.reset();
						 			PID_timer.start();
					 				r_pidController.enable();
					 				l_pidController.enable();
					 				r_pidController.setSetpoint(12); //52
					 				l_pidController.setSetpoint(12);//52
					 				
					 				autoLock1 = false;
					 				autoLock2 = true;
					 			}
					 			MPC1.set(0);
					 			MPC2.set(0);
					 			
					 			if(r_pidController.onTarget() == true && l_pidController.onTarget() == true) {
					 				ordreAuto = 12;
					 			}
					 			
					 			if(PID_timer.get() >= 0.9) {
					 				ordreAuto = 12;
					 			}
					 	
					 		}else	if(ordreAuto == 12) {
					 			if(autoLock1 == false) {
					 				ahrs.reset();
					 				encDroit.reset();
					 				encGauche.reset();
					 				
					 				r_pidController.disable();
					 				l_pidController.disable();
					 				autoLock2 = false;
					 				autoLock1 = true;
					 			}
					 			double turningValue = -ahrs.getAngle();
								if(turningValue > 0.3) {
									turningValue = 0.3;
								}
								if (turningValue < -0.3) {
									turningValue = -0.3;
								}
					 			
					 		 	m_robotDrive.arcadeDrive(0.6, 0);
						
					 		 	
					 		 	if(roue >= 0.3) {
					 		 		ordreAuto = 13;
					 		 	}
							}else if(ordreAuto == 13) {
								if(autoLock2 == false) {
									//PID_timer.start();
									retourTimer.reset();
									ahrs.reset();
									encGauche.reset();
									encDroit.reset();
									PID_timer.reset();
									PID_timer.start();
									r_pidController.enable();
									l_pidController.enable();
									r_pidController.setSetpoint(40); //48
									l_pidController.setSetpoint(40);
							
									autoLock2 = true;
									autoLock1 = false;
								}
								
								if(PID_timer.get() >= 0.9) {
									ordreAuto = 14;
									r_pidController.disable();
									l_pidController.disable();
								}
								
							} else if(ordreAuto == 14) {
								m_robotDrive.arcadeDrive(0.6, 0);
								
								if(retourTimer.get() >= 2) {
									ordreAuto = 15;
								}
							} else if (ordreAuto == 15) {
								m_robotDrive.arcadeDrive(0, 0);
							}
				 				
					 		
					 		
					 		}
							SmartDashboard.putNumber("ordreAuto", ordreAuto);
				
			break;
			
		case kCustomAuto2:

			 SmartDashboard.putNumber("Ok",1);
			 
			//--------------------------Variables auto 1-----------------------------------

							
								 countGauche = encGauche.get();
								 countDroit = encDroit.get();
								 roueGauche = -countGauche/240;
								roueDroit = -countDroit/240;
								 rouefalse = roueGauche - roueDroit;
								 roue = rouefalse / 2;
						//480 = un tour
								SmartDashboard.putNumber("Tours", roue);
						
						
			//----------------------Debut mode autonome 1--------------------------
						//roue  < 2.5
						if(retourAuto == false && ordreAuto == 0) {
							
							double turningValue = -ahrs.getAngle();
							SmartDashboard.putNumber("Turning Value", turningValue);

							if(turningValue > 0.3) {
								turningValue = 0.3;
							}
							
							else if (turningValue < -0.3) {
								turningValue = -0.3;
							}

							m_robotDrive.arcadeDrive(0.65, -turningValue * 0.7);
							
								if(roue >= 2.2) {
									ordreAuto = 1;
								}
								
						//PID_timer.get() < 0.8
						}else if (retourAuto == false && ordreAuto == 1 ){
							if(autoLock1 == false) {
								//PID_timer.start();
								ahrs.reset();
								encGauche.reset();
								encDroit.reset();
								PID_timer.start();
								r_pidController.enable();
								l_pidController.enable();
								r_pidController.setSetpoint(-45); //48
								l_pidController.setSetpoint(-45);
								r_pidController.setOutputRange(-0.7, 0.7); 
								l_pidController.setOutputRange(-0.7, 0.7); 
								r_pidController.setAbsoluteTolerance(0.001);
								l_pidController.setAbsoluteTolerance(0.001);
								autoLock1 = true;
								autoLock2 = false;
							}
							SmartDashboard.putNumber("PId gyro value", ahrs.getAngle());
							if(r_pidController.onTarget() == true && l_pidController.onTarget() == true) {
								ordreAuto = 2;
								
							}else if(PID_timer.get() >= 1.2) {
								ordreAuto = 2;
								PID_timer.stop();
								PID_timer.reset();
							}
							
						//PID_timer.get() >= 0.8 && roue < 4.75
						}else if (retourAuto == false && ordreAuto == 2) {
							if(autoLock2 == false) {
							encGauche.reset();
							encDroit.reset();
								ahrs.reset();
								PID_timer.stop();
							
								r_pidController.disable();
								l_pidController.disable();
								
								pneuPince.set(DoubleSolenoid.Value.kForward);
								encPince_pidController.enable();
								encPince_pidController.setSetpoint(700);
							
								
								autoLock2 = true;
								autoLock1 = false;
							}
							
							double turningValue = -ahrs.getAngle() ;
							SmartDashboard.putNumber("Turning Value", turningValue);
							
							if(turningValue > 0.5) {
								turningValue = 0.5;
							}
							if (turningValue < -0.5) {
								turningValue = -0.5;
							}
							
							m_robotDrive.arcadeDrive(0.75, -turningValue * 0.7);
							
							if(roue >= 6.75) {
								ordreAuto = 3;
							}
							//PID_timer.get() < 1
						}else if ( retourAuto == false && ordreAuto == 3){
							if(autoLock1 == false) {
								PID_timer.reset();
								PID_timer.start();
								ahrs.reset();
								
								limit_timer.start();
								r_pidController.enable();
								l_pidController.enable();
								r_pidController.setAbsoluteTolerance(0.0001);
								l_pidController.setAbsoluteTolerance(0.0001);
								r_pidController.setSetpoint(-90);
								l_pidController.setSetpoint(-90);
								
								autoLock1 = true;
								autoLock2 = false;
								

							}
							if(PID_timer.get() >= 1.21) {
								ordreAuto = 4;
							}
							SmartDashboard.putNumber("Pid Timer",PID_timer.get());
							if(r_pidController.onTarget() == true && l_pidController.onTarget() == true) 
							{
								ordreAuto = 4;
							}
							//limit_timer.get() <=1.2 && PID_timer.get() >= 1
						}else if (retourAuto == false  && ordreAuto == 4){
							if(autoLock2 == false) {
							
								ahrs.reset();
								PID_timer.stop();
								r_pidController.disable();
								l_pidController.disable();
								SmartDashboard.putString("PidDisabled","Yes");
								
								autoLock2 = true;
								autoLock1 = false;
						}
							
							double turningValue = -ahrs.getAngle() ;
							SmartDashboard.putNumber("Turning Value", turningValue);
							
							if(turningValue > 0.5) {
								turningValue = 0.5;
							}
							if (turningValue < -0.5) {
								turningValue = -0.5;
							}
						
						//	m_robotDrive.arcadeDrive(0, -turningValue * 0.8);
							
							if(0 == 0) {
								ordreAuto = 5;
							}
							//roue  <= 10.5 
						}else if (ordreAuto == 5 || retourAuto == true ){
							
							retourAuto = true;
					
					if(limitswitchPince.get() == true && autoPince == false) {
								
								double[] gripValuesX = new double[0];
								double[] XValue =  xCenterEntry.getDoubleArray(gripValuesX);
								double angleVise = 320 * 7 / 147; // /10.33

							
								
								
								MPC1.set(0.45);
							MPC2.set(-0.45);
								
								if(XValue.length != 0) {

										if(premiereMesure == false) {
											encGauche.reset();
											encDroit.reset(); // /20.102
											ahrs.reset();
											double trouve =  XValue[0] * 7 / 147; //10.33 //7/127
											SmartDashboard.putNumber("Trouve", trouve);
											

											
									
											premiereMesure = true;  
											
											calcul = trouve - angleVise;
											SmartDashboard.putNumber("Calculs", calcul);
										}
										
										if(premiereMesure == true) {
											
											double angleGyro = ahrs.getAngle();
											SmartDashboard.putNumber("Angle Gyro", angleGyro);
											double setPoint =  calcul - angleGyro ;
											SmartDashboard.putNumber("Set Point", setPoint);
											
											double setPointTrue = -setPoint * 0.8;
											if(setPointTrue > 0.5) {
												setPointTrue = 0.5;
											}
											if (setPointTrue < -0.5) {
												setPointTrue = -0.5;
											}
											m_robotDrive.arcadeDrive(-0.575, setPointTrue); //0.025
										}
			                                                                                         
								}else if (premiereMesure == true) {
									double angleGyro = ahrs.getAngle();
									SmartDashboard.putNumber("Angle Gyro v2", angleGyro);
									double setPoint =  calcul - angleGyro ;
									SmartDashboard.putNumber("Set Point v2", setPoint);
									double setPointTrue = -setPoint * 0.8;
									if(setPointTrue > 0.5) {
										setPointTrue = 0.5;
									}
									if (setPointTrue < -0.5) {
										setPointTrue = -0.5;
									}
									m_robotDrive.arcadeDrive(-0.6,	setPointTrue); //0.025
						
								}else {
									m_robotDrive.arcadeDrive(0, 0.4);
							}
								//roue < 0.3
							}else if( ordreAuto == 5) {
								MPC1.set(0.15);
								MPC2.set(-0.15);
								autoPince = true;

				 			
								double turningValue = ahrs.getAngle();
							
								if(turningValue > 0.5) {
									turningValue = 0.5;
								}
								if (turningValue < -0.5) {
									turningValue = -0.5;
								}
				 			
								m_robotDrive.arcadeDrive(0.65, turningValue );
				 			
								if(roue >= -0.18) {
									ordreAuto = 6;
								}
								//retourTimer.get() < 1
							} else if (ordreAuto == 6) {
								MPC1.set(0.55);
								MPC2.set(-0.55);
								if(autoLock1 == false) {
									ahrs.reset();
									PID_timer.reset();
									PID_timer.start();
									
									
									retourTimer.start();
									r_pidController.enable();
									l_pidController.enable();
									r_pidController.setSetpoint(90);
									l_pidController.setSetpoint(90);
									
									r_pidController.setAbsoluteTolerance(0.001);
									l_pidController.setAbsoluteTolerance(0.001);
									autoLock1 = true;
									autoLock2 = false;
								}
								
								if(r_pidController.onTarget() == true && l_pidController.onTarget() == true) {
									ordreAuto = 7;
								}
								if(PID_timer.get() >= 1.2) {
									ordreAuto = 7;
								}
								
							//roue > -5.5 
				 		} else if (ordreAuto == 7) {
				 			MPC1.set(0.15);
				 			MPC2.set(-0.15);
				 			r_pidController.disable();
				 			l_pidController.disable();
							double turningValue = -ahrs.getAngle();
							SmartDashboard.putNumber("Turning Value", turningValue);
							
							if(turningValue > 0.3) {
								turningValue = 0.3;
							}
							if (turningValue < -0.3) {
								turningValue = -0.3;
							}
							m_robotDrive.arcadeDrive(-0.7, 0);
							
							if(roue <= -5.8) {
								ordreAuto = 8;
							}
							//retourTimer.get() < 3.5
				 		} else if (ordreAuto == 8) {

				 			MPC1.set(0.15);
				 			MPC2.set(-0.15);
				 			if(autoLock2 == false) {
				 				ahrs.reset();
					 			PID_timer.reset();
					 			PID_timer.start();
				 				r_pidController.enable();
				 				l_pidController.enable();
				 				r_pidController.setSetpoint(52);
				 				l_pidController.setSetpoint(52);
				 				
				 				autoLock2 = true;
				 				autoLock1 = false;
				 			}
				 			
				 			if(r_pidController.onTarget() == true && l_pidController.onTarget() == true) {
				 				ordreAuto = 9;
				 			}
				 			
				 			if(PID_timer.get() >= 0.9) {
				 				ordreAuto = 9;
				 			}
				 		} else if (ordreAuto == 9) {
				 			if(autoLock1 == false) {
				 				retourTimer.reset();
				 				retourTimer.start();
				 				
				 				autoLock1 = true;
				 				autoLock2 = false;
				 			}
				 			MPC1.set(0.15);
				 			MPC2.set(-0.15);
				 			r_pidController.disable();
				 			l_pidController.disable();
							double turningValue = -ahrs.getAngle();
							SmartDashboard.putNumber("Turning Value", turningValue);
							
							if(turningValue > 0.3) {
								turningValue = 0.3;
							}
							if (turningValue < -0.3) {
								turningValue = -0.3;
							}
							m_robotDrive.arcadeDrive(-0.6, 0);
							
							if(retourTimer.get() >= 0.3) {
								ordreAuto = 10;
							}
				 		}else if (ordreAuto == 10) {
							MPC1.set(-0.75);
							MPC2.set(0.75);
							
							if(retourTimer.get() >= 0.5) {
								ordreAuto = 11;
							}
				 		}else if(ordreAuto == 11) {
				 			if(autoLock2 == false) {
				 				ahrs.reset();
				 				retourTimer.reset();
				 				retourTimer.start();
				 				

				 				
					 			PID_timer.reset();
					 			PID_timer.start();
				 				r_pidController.enable();
				 				l_pidController.enable();
				 				r_pidController.setSetpoint(-12); //52
				 				l_pidController.setSetpoint(-12);//52
				 				
				 				autoLock1 = false;
				 				autoLock2 = true;
				 			}
				 			MPC1.set(0);
				 			MPC2.set(0);
				 			
				 			if(r_pidController.onTarget() == true && l_pidController.onTarget() == true) {
				 				ordreAuto = 12;
				 			}
				 			
				 			if(PID_timer.get() >= 0.9) {
				 				ordreAuto = 12;
				 			}
				 	
				 		}else	if(ordreAuto == 12) {
				 			if(autoLock1 == false) {
				 				ahrs.reset();
				 				encDroit.reset();
				 				encGauche.reset();
				 				
				 				r_pidController.disable();
				 				l_pidController.disable();
				 				autoLock2 = false;
				 				autoLock1 = true;
				 			}
				 			double turningValue = -ahrs.getAngle();
							if(turningValue > 0.3) {
								turningValue = 0.3;
							}
							if (turningValue < -0.3) {
								turningValue = -0.3;
							}
				 			
				 		 	m_robotDrive.arcadeDrive(0.6, 0);
					
				 		 	
				 		 	if(roue >= 0.2) {
				 		 		ordreAuto = 13;
				 		 	}
						}else if(ordreAuto == 13) {
							if(autoLock2 == false) {
								//PID_timer.start();
								retourTimer.reset();
								ahrs.reset();
								encGauche.reset();
								encDroit.reset();
								PID_timer.reset();
								PID_timer.start();
								r_pidController.enable();
								l_pidController.enable();
								r_pidController.setSetpoint(-40); //48
								l_pidController.setSetpoint(-40);
						
								autoLock2 = true;
								autoLock1 = false;
							}
							
							if(PID_timer.get() >= 0.9) {
								ordreAuto = 14;
								r_pidController.disable();
								l_pidController.disable();
							}
							
						} else if(ordreAuto == 14) {
							m_robotDrive.arcadeDrive(0.6, 0);
							
							if(retourTimer.get() >= 2) {
								ordreAuto = 15;
							}
						} else if (ordreAuto == 15) {
							m_robotDrive.arcadeDrive(0, 0);
						}
			 				
				 		
				 		
				 		}
						SmartDashboard.putNumber("ordreAuto", ordreAuto);
			
			break;
			
		case kCustomAuto3:

			//--------------------------Variables auto 1-----------------------------------
			 
			countGauche = encGauche.get();
			countDroit = encDroit.get();
			roueGauche = -countGauche/240;
			 roueDroit = -countDroit/240;
			rouefalse = roueGauche - roueDroit;
		roue = rouefalse / 2;
	//480 = un tour
			SmartDashboard.putNumber("Tours", roue);
	
	
//----------------------Debut mode autonome 1--------------------------
			if (retourAuto == false && ordreAuto == 0) {
				double turningValue = -ahrs.getAngle();
				SmartDashboard.putNumber("Turning Value", turningValue);

				if(turningValue > 0.3) {
					turningValue = 0.3;
				}
				
				else if (turningValue < -0.3) {
					turningValue = -0.3;
				}

				m_robotDrive.arcadeDrive(0.75, -turningValue * 0.7);	
			
				if (roue >= 6.75 ) { //6.3 // 7.2
					ordreAuto = 1;
				}
		
			}
			
		
			
	//PID_t	imer.get() < 0.8
		else if (retourAuto == false && ordreAuto == 1 ){
				if(autoLock1 == false) {
				//PID_timer.start();
				ahrs.reset();
				encGauche.reset();
				encDroit.reset();
				PID_timer.start();
				r_pidController.enable();
				l_pidController.enable();
				r_pidController.setSetpoint(77);
				l_pidController.setSetpoint(77);
				r_pidController.setOutputRange(-0.8, 0.8);
				l_pidController.setOutputRange(-0.8, 0.8);
				r_pidController.setAbsoluteTolerance(0.001);
				l_pidController.setAbsoluteTolerance(0.001);
				autoLock1 = true;
				autoLock2 = false;
			}
			SmartDashboard.putNumber("PId gyro value", ahrs.getAngle());
			if(r_pidController.onTarget() == true && l_pidController.onTarget() == true) {
				ordreAuto = 2;
				
			}else if(PID_timer.get() >= 1.2) {
				ordreAuto = 2;
				PID_timer.stop();
				PID_timer.reset();
			}
			
		//PID_timer.get() >= 0.8 && roue < 4.75
		}else if (retourAuto == false && ordreAuto == 2) {
			if(autoLock2 == false) {
			encGauche.reset();
			encDroit.reset();
				ahrs.reset();
				PID_timer.stop();
			
				r_pidController.disable();
				l_pidController.disable();
				
				pneuPince.set(DoubleSolenoid.Value.kForward);
				encPince_pidController.enable();
				encPince_pidController.setSetpoint(700);
				
				autoLock2 = true;
				autoLock1 = false;
				
				
			}
			
			double turningValue = -ahrs.getAngle() ;
			SmartDashboard.putNumber("Turning Value", turningValue);
			
			if(turningValue > 0.5) {
				turningValue = 0.5;
			}
			if (turningValue < -0.5) {
				turningValue = -0.5;
			}
			
			m_robotDrive.arcadeDrive(0.72, -turningValue * 0.7);
			
			if(roue >= 4.65 ) { //4.6 //5.2
 				ordreAuto = 3;
			}
			//PID_timer.get() < 1
		}else if ( retourAuto == false && ordreAuto == 3){
			if(autoLock1 == false) {
				PID_timer.reset();
				PID_timer.start();
				ahrs.reset();
				
				limit_timer.start();
				r_pidController.enable();
				l_pidController.enable();
				r_pidController.setAbsoluteTolerance(0.005);
				l_pidController.setAbsoluteTolerance(0.005);
				r_pidController.setSetpoint(55);
				l_pidController.setSetpoint(55);
				
				autoLock1 = true;
				autoLock2 = false;
			}
			
			if(PID_timer.get() >= 1.2) {
				ordreAuto = 4;
			}
			SmartDashboard.putNumber("Pid Timer",PID_timer.get());
			
			if(r_pidController.onTarget() == true && l_pidController.onTarget() == true) 
			{
				ordreAuto = 4;
			}
			
		}else if (retourAuto == false  && ordreAuto == 4){
				if(autoLock2 == false) {
				
					ahrs.reset();
					PID_timer.stop();
					r_pidController.disable();
					l_pidController.disable();
					SmartDashboard.putString("PidDisabled","Yes");
					
					autoLock2 = true;
					autoLock1 = false;
			}
				
				//double turningValue = -ahrs.getAngle() ;
				//SmartDashboard.putNumber("Turning Value", turningValue);
				
		/*		if(turningValue > 0.5) {
					turningValue = 0.5;
				}
				if (turningValue < -0.5) {
					turningValue = -0.5;
				}
			
			//	m_robotDrive.arcadeDrive(0, -turningValue * 0.7);
			*/	
				if(0 == 0) {
					ordreAuto = 5;
				}
			}else if (ordreAuto == 5 || retourAuto == true ){
				
				retourAuto = true;
		
		if(limitswitchPince.get() == true && autoPince == false) {
					
					double[] gripValuesX = new double[0];
					double[] XValue =  xCenterEntry.getDoubleArray(gripValuesX);
					double angleVise = 320 / 20; // /10.33

				
					
					
					MPC1.set(0.45);
				MPC2.set(-0.45);
					
					if(XValue.length != 0) {

							if(premiereMesure == false) {
								encGauche.reset();
								encDroit.reset();
								ahrs.reset();
								double trouve =  XValue[0] / 20; //10.33
								SmartDashboard.putNumber("Trouve", trouve);
								

								
						
								premiereMesure = true;  
								
								calcul = trouve - angleVise;
								SmartDashboard.putNumber("Calculs", calcul);
							}
							
							if(premiereMesure == true) {
								
								double angleGyro = ahrs.getAngle();
								SmartDashboard.putNumber("Angle Gyro", angleGyro);
								double setPoint =  calcul - angleGyro ;
								SmartDashboard.putNumber("Set Point", setPoint);
								
								double setPointTrue = -setPoint * 0.7;
								if(setPointTrue > 0.5) {
									setPointTrue = 0.5;
								}
								if (setPointTrue < -0.5) {
									setPointTrue = -0.5;
								}
								m_robotDrive.arcadeDrive(-0.6, setPointTrue); //0.025
							}
                                                                                         
					}else if (premiereMesure == true) {
						double angleGyro = ahrs.getAngle();
						SmartDashboard.putNumber("Angle Gyro v2", angleGyro);
						double setPoint =  calcul - angleGyro ;
						SmartDashboard.putNumber("Set Point v2", setPoint);
						double setPointTrue = -setPoint * 0.7;
						if(setPointTrue > 0.5) {
							setPointTrue = 0.5;
						}
						if (setPointTrue < -0.5) {
							setPointTrue = -0.5;
						}
						m_robotDrive.arcadeDrive(-0.6,	setPointTrue); //0.025
			
					}else {
						m_robotDrive.arcadeDrive(0, -0.4);
				}
					//roue < 0.3
				}else if( ordreAuto == 5) {
					MPC1.set(0.15);
					MPC2.set(-0.15);
					autoPince = true;

	 			
					double turningValue = ahrs.getAngle();
				
					if(turningValue > 0.5) {
						turningValue = 0.5;
					}
					if (turningValue < -0.5) {
						turningValue = -0.5;
					}
	 			
					m_robotDrive.arcadeDrive(0.7, turningValue );
	 			
					if(roue >= 0) {
						ordreAuto = 6;
					}
				} else if (ordreAuto == 6) {
					MPC1.set(0.55);
					MPC2.set(-0.55);
					if(autoLock1 == false) {
						ahrs.reset();
						PID_timer.reset();
						PID_timer.start();
						
						
						retourTimer.start();
						r_pidController.enable();
						l_pidController.enable();
						r_pidController.setSetpoint(-50);
						l_pidController.setSetpoint(-50);

						r_pidController.setAbsoluteTolerance(0.001);
						l_pidController.setAbsoluteTolerance(0.001);
						autoLock1 = true;
						autoLock2 = false;
					}
					
					if(r_pidController.onTarget() == true && l_pidController.onTarget() == true) {
						ordreAuto = 7;
					}
					if(PID_timer.get() >= 0.9) {
						ordreAuto = 7;
					}
				} else if (ordreAuto == 7) {
		 			MPC1.set(0.15);
		 			MPC2.set(-0.15);
		 			r_pidController.disable();
		 			l_pidController.disable();
					double turningValue = -ahrs.getAngle();
					SmartDashboard.putNumber("Turning Value", turningValue);
					
					if(turningValue > 0.3) {
						turningValue = 0.3;
					}
					if (turningValue < -0.3) {
						turningValue = -0.3;
					}
					m_robotDrive.arcadeDrive(-0.8, 0);
					
					if(roue <= -4.5) {
						ordreAuto = 8;
					}
					//retourTimer.get() < 3.5
		 		} else if (ordreAuto == 8) {

		 			MPC1.set(0.2);
		 			MPC2.set(-0.2);
		 			if(autoLock2 == false) {
		 				
		 				ahrs.reset();
			 			PID_timer.reset();
			 			PID_timer.start();
		 				r_pidController.enable();
		 				l_pidController.enable();
		 				r_pidController.setSetpoint(-75);
		 				l_pidController.setSetpoint(-75);
		 				
		 				autoLock2 = true;
		 				autoLock1 = false;
		 			}
		 			
		 			if(r_pidController.onTarget() == true && l_pidController.onTarget() == true) {
		 				ordreAuto = 9;
		 			}
		 			
		 			if(PID_timer.get() >= 0.9) {
		 				ordreAuto = 9;
		 			}
		 		} else if (ordreAuto == 9) {
		 			if(autoLock1 == false) {
		 				retourTimer.reset();
		 				retourTimer.start();
		 				
		 				autoLock1 = true;
		 				autoLock2 = false;
		 			}
		 			MPC1.set(0.15);
		 			MPC2.set(-0.15);
		 			r_pidController.disable();
		 			l_pidController.disable();
					double turningValue = -ahrs.getAngle();
					SmartDashboard.putNumber("Turning Value", turningValue);
					
					if(turningValue > 0.3) {
						turningValue = 0.3;
					}
					if (turningValue < -0.3) {
						turningValue = -0.3;
					}
					m_robotDrive.arcadeDrive(-0.7, 0);
					
					if(roue <= -9.2) {
						ordreAuto = 10;
					}
		 		}else if (ordreAuto == 10) {
					MPC1.set(-0.9);
					MPC2.set(0.9);
					
					if(retourTimer.get() >= 0.5) {
						ordreAuto = 11;
					}
					
		 		}else if(ordreAuto == 11) {
		 			MPC1.set(0);
		 			MPC2.set(0);
		 			ordreAuto = 12;
		 	
		 		}else	if(ordreAuto == 12) {
		 			if(autoLock2 == false) {
		 				ahrs.reset();
		 				
		 				
		 				autoLock2 = true;
		 				autoLock1 = false;
		 			}
		 			double turningValue = -ahrs.getAngle();
					if(turningValue > 0.3) {
						turningValue = 0.3;
					}
					if (turningValue < -0.3) {
						turningValue = -0.3;
					}
		 			
		 		 	m_robotDrive.arcadeDrive(0.6, 0);
			
				}
	 				
		 		
		 		
		 		}
			
	
				
					
					
					
		
			SmartDashboard.putNumber("ordreAuto", ordreAuto);
		break;			
			
			
		case kCustomAuto4:

			//--------------------------Variables auto 1-----------------------------------
			 
			countGauche = encGauche.get();
			countDroit = encDroit.get();
			roueGauche = -countGauche/240;
			 roueDroit = -countDroit/240;
			rouefalse = roueGauche - roueDroit;
		roue = rouefalse / 2;
	//480 = un tour
			SmartDashboard.putNumber("Tours", roue);
	
	
//----------------------Debut mode autonome 1--------------------------
			if (retourAuto == false && ordreAuto == 0) {
				double turningValue = -ahrs.getAngle();
				SmartDashboard.putNumber("Turning Value", turningValue);

				if(turningValue > 0.3) {
					turningValue = 0.3;
				}
				
				else if (turningValue < -0.3) {
					turningValue = -0.3;
				}

				m_robotDrive.arcadeDrive(0.75, -turningValue * 0.7);	
			
				if (roue >= 6.75 ) { //6.3 // 7.2
					ordreAuto = 1;
				}
		
			}
			
		
			
	//PID_t	imer.get() < 0.8
		else if (retourAuto == false && ordreAuto == 1 ){
				if(autoLock1 == false) {
				//PID_timer.start();
				ahrs.reset();
				encGauche.reset();
				encDroit.reset();
				PID_timer.start();
				r_pidController.enable();
				l_pidController.enable();
				r_pidController.setSetpoint(-79);
				l_pidController.setSetpoint(-79);
				r_pidController.setOutputRange(-0.8, 0.8);
				l_pidController.setOutputRange(-0.8, 0.8);
				r_pidController.setAbsoluteTolerance(0.01);
				l_pidController.setAbsoluteTolerance(0.01);
				autoLock1 = true;
				autoLock2 = false;
			}
			SmartDashboard.putNumber("PId gyro value", ahrs.getAngle());
			if(r_pidController.onTarget() == true && l_pidController.onTarget() == true) {
				ordreAuto = 2;
				
			}else if(PID_timer.get() >= 1.2) {
				ordreAuto = 2;
				PID_timer.stop();
				PID_timer.reset();
			}
			
		//PID_timer.get() >= 0.8 && roue < 4.75
		}else if (retourAuto == false && ordreAuto == 2) {
			if(autoLock2 == false) {
			encGauche.reset();
			encDroit.reset();
				ahrs.reset();
				PID_timer.stop();
			
				r_pidController.disable();
				l_pidController.disable();
				
				pneuPince.set(DoubleSolenoid.Value.kForward);
				encPince_pidController.enable();
				encPince_pidController.setSetpoint(700);
				
				autoLock2 = true;
				autoLock1 = false;
				
				
			}
			
			double turningValue = -ahrs.getAngle() ;
			SmartDashboard.putNumber("Turning Value", turningValue);
			
			if(turningValue > 0.5) {
				turningValue = 0.5;
			}
			if (turningValue < -0.5) {
				turningValue = -0.5;
			}
			
			m_robotDrive.arcadeDrive(0.72, -turningValue * 0.7);
			
			if(roue >= 5.26 ) { //4.6 //5.2
 				ordreAuto = 3;
			}
			//PID_timer.get() < 1
		}else if ( retourAuto == false && ordreAuto == 3){
			if(autoLock1 == false) {
				PID_timer.reset();
				PID_timer.start();
				ahrs.reset();
				
				limit_timer.start();
				r_pidController.enable();
				l_pidController.enable();
				r_pidController.setAbsoluteTolerance(0.005);
				l_pidController.setAbsoluteTolerance(0.005);
				r_pidController.setSetpoint(-60);
				l_pidController.setSetpoint(-60);
				
				autoLock1 = true;
				autoLock2 = false;
			}
			
			if(PID_timer.get() >= 1.2) {
				ordreAuto = 4;
			}
			SmartDashboard.putNumber("Pid Timer",PID_timer.get());
			
			if(r_pidController.onTarget() == true && l_pidController.onTarget() == true) 
			{
				ordreAuto = 4;
			}
			
		}else if (retourAuto == false  && ordreAuto == 4){
				if(autoLock2 == false) {
				
					ahrs.reset();
					PID_timer.stop();
					r_pidController.disable();
					l_pidController.disable();
					SmartDashboard.putString("PidDisabled","Yes");
					
					autoLock2 = true;
					autoLock1 = false;
			}
				
				//double turningValue = -ahrs.getAngle() ;
				//SmartDashboard.putNumber("Turning Value", turningValue);
				
		/*		if(turningValue > 0.5) {
					turningValue = 0.5;
				}
				if (turningValue < -0.5) {
					turningValue = -0.5;
				}
			
			//	m_robotDrive.arcadeDrive(0, -turningValue * 0.7);
			*/	
				if(0 == 0) {
					ordreAuto = 5;
				}
			}else if (ordreAuto == 5 || retourAuto == true ){
				
				retourAuto = true;
		
		if(limitswitchPince.get() == true && autoPince == false) {
					
					double[] gripValuesX = new double[0];
					double[] XValue =  xCenterEntry.getDoubleArray(gripValuesX);
					double angleVise = 320 /20.102; // /10.33

				
					
					
					MPC1.set(0.45);
				MPC2.set(-0.45);
					
					if(XValue.length != 0) {

							if(premiereMesure == false) {
								encGauche.reset();
								encDroit.reset();
								ahrs.reset();
								double trouve =  XValue[0] /20.102; //10.33
								SmartDashboard.putNumber("Trouve", trouve);
								

								
						
								premiereMesure = true;  
								
								calcul = trouve - angleVise;
								SmartDashboard.putNumber("Calculs", calcul);
							}
							
							if(premiereMesure == true) {
								
								double angleGyro = ahrs.getAngle();
								SmartDashboard.putNumber("Angle Gyro", angleGyro);
								double setPoint =  calcul - angleGyro ;
								SmartDashboard.putNumber("Set Point", setPoint);
								
								double setPointTrue = -setPoint * 0.7;
								if(setPointTrue > 0.5) {
									setPointTrue = 0.5;
								}
								if (setPointTrue < -0.5) {
									setPointTrue = -0.5;
								}
								m_robotDrive.arcadeDrive(-0.6, setPointTrue); //0.025
							}
                                                                                         
					}else if (premiereMesure == true) {
						double angleGyro = ahrs.getAngle();
						SmartDashboard.putNumber("Angle Gyro v2", angleGyro);
						double setPoint =  calcul - angleGyro ;
						SmartDashboard.putNumber("Set Point v2", setPoint);
						double setPointTrue = -setPoint * 0.7;
						if(setPointTrue > 0.5) {
							setPointTrue = 0.5;
						}
						if (setPointTrue < -0.5) {
							setPointTrue = -0.5;
						}
						m_robotDrive.arcadeDrive(-0.6,	setPointTrue); //0.025
			
					}else {
						m_robotDrive.arcadeDrive(0, -0.4);
				}
					//roue < 0.3
				}else if( ordreAuto == 5) {
					MPC1.set(0.15);
					MPC2.set(-0.15);
					autoPince = true;

	 			
					double turningValue = ahrs.getAngle();
				
					if(turningValue > 0.5) {
						turningValue = 0.5;
					}
					if (turningValue < -0.5) {
						turningValue = -0.5;
					}
	 			
					m_robotDrive.arcadeDrive(0.7, turningValue );
	 			
					if(roue >= 0.25) {
						ordreAuto = 6;
					}
				} else if (ordreAuto == 6) {
					MPC1.set(0.55);
					MPC2.set(-0.55);
					if(autoLock1 == false) {
						ahrs.reset();
						PID_timer.reset();
						PID_timer.start();
						
						
						retourTimer.start();
						r_pidController.enable();
						l_pidController.enable();
						r_pidController.setSetpoint(50);
						l_pidController.setSetpoint(50);

						r_pidController.setAbsoluteTolerance(0.001);
						l_pidController.setAbsoluteTolerance(0.001);
						autoLock1 = true;
						autoLock2 = false;
					}
					
					if(r_pidController.onTarget() == true && l_pidController.onTarget() == true) {
						ordreAuto = 7;
					}
					if(PID_timer.get() >= 0.9) {
						ordreAuto = 7;
					}
				} else if (ordreAuto == 7) {
		 			MPC1.set(0.15);
		 			MPC2.set(-0.15);
		 			r_pidController.disable();
		 			l_pidController.disable();
					double turningValue = -ahrs.getAngle();
					SmartDashboard.putNumber("Turning Value", turningValue);
					
					if(turningValue > 0.3) {
						turningValue = 0.3;
					}
					if (turningValue < -0.3) {
						turningValue = -0.3;
					}
					m_robotDrive.arcadeDrive(-0.8, 0);
					
					if(roue <= -3.4) {
						ordreAuto = 8;
					}
					//retourTimer.get() < 3.5
		 		} else if (ordreAuto == 8) {

		 			MPC1.set(0.2);
		 			MPC2.set(-0.2);
		 			if(autoLock2 == false) {
		 				
		 				ahrs.reset();
			 			PID_timer.reset();
			 			PID_timer.start();
		 				r_pidController.enable();
		 				l_pidController.enable();
		 				r_pidController.setSetpoint(95);
		 				l_pidController.setSetpoint(95);
		 				
		 				autoLock2 = true;
		 				autoLock1 = false;
		 			}
		 			
		 			if(r_pidController.onTarget() == true && l_pidController.onTarget() == true) {
		 				ordreAuto = 9;
		 			}
		 			
		 			if(PID_timer.get() >= 0.9) {
		 				ordreAuto = 9;
		 			}
		 		} else if (ordreAuto == 9) {
		 			if(autoLock1 == false) {
		 				retourTimer.reset();
		 				retourTimer.start();
		 				
		 				autoLock1 = true;
		 				autoLock2 = false;
		 			}
		 			MPC1.set(0.15);
		 			MPC2.set(-0.15);
		 			r_pidController.disable();
		 			l_pidController.disable();
					double turningValue = -ahrs.getAngle();
					SmartDashboard.putNumber("Turning Value", turningValue);
					
					if(turningValue > 0.3) {
						turningValue = 0.3;
					}
					if (turningValue < -0.3) {
						turningValue = -0.3;
					}
					m_robotDrive.arcadeDrive(-0.7, 0);
					
					if(roue <= -9) {
						ordreAuto = 10;
					}
		 		}else if (ordreAuto == 10) {
					MPC1.set(-0.9);
					MPC2.set(0.9);
					
					if(retourTimer.get() >= 0.5) {
						ordreAuto = 11;
					}
					
		 		}else if(ordreAuto == 11) {
		 			MPC1.set(0);
		 			MPC2.set(0);
		 			ordreAuto = 12;
		 	
		 		}else	if(ordreAuto == 12) {
		 			if(autoLock2 == false) {
		 				ahrs.reset();
		 				
		 				
		 				autoLock2 = true;
		 				autoLock1 = false;
		 			}
		 			double turningValue = -ahrs.getAngle();
					if(turningValue > 0.3) {
						turningValue = 0.3;
					}
					if (turningValue < -0.3) {
						turningValue = -0.3;
					}
		 			
		 		 	m_robotDrive.arcadeDrive(0.6, 0);
			
				}
	 				
		 		
		 		
		 		}
			
	
				
					
					
					
		
			SmartDashboard.putNumber("ordreAuto", ordreAuto);
			break;
			
		case kCustomAuto5:
			
			//--------------------------Variables auto 1-----------------------------------
			countGauche = encGauche.get();
			countDroit = encDroit.get();
			roueGauche = -countGauche/240;
			 roueDroit = -countDroit/240;
			rouefalse = roueGauche - roueDroit;
		roue = rouefalse / 2;
	//480 = un tour
			SmartDashboard.putNumber("Tours", roue);
	
	
//----------------------Debut mode autonome 1--------------------------
			if (retourAuto == false && ordreAuto == 0) {
				double turningValue = -ahrs.getAngle();
				SmartDashboard.putNumber("Turning Value", turningValue);

				if(turningValue > 0.3) {
					turningValue = 0.3;
				}
				
				else if (turningValue < -0.3) {
					turningValue = -0.3;
				}

				m_robotDrive.arcadeDrive(0.65, -turningValue * 0.7);	
			
			
				pneuPince.set(DoubleSolenoid.Value.kForward);
				encPince_pidController.enable();
				encPince_pidController.setSetpoint(700);
				
				if (roue >= 12.5 ) {  //12
				ordreAuto = 1;
			}
			
	//PID_t	imer.get() < 0.8
			}else if (retourAuto == false && ordreAuto == 1 ){
				if(autoLock1 == false) {
				//PID_timer.start();
				ahrs.reset();
				encGauche.reset();
				encDroit.reset();
				PID_timer.start();
				r_pidController.enable();
				l_pidController.enable();
				r_pidController.setSetpoint(-48);
				l_pidController.setSetpoint(-48);
				r_pidController.setOutputRange(-0.75, 0.75);
				l_pidController.setOutputRange(-0.75, 0.75);
				r_pidController.setAbsoluteTolerance(0.001);
				l_pidController.setAbsoluteTolerance(0.001);
				autoLock1 = true;
				autoLock2 = false;
			}
			SmartDashboard.putNumber("PId gyro value", ahrs.getAngle());
			if(r_pidController.onTarget() == true && l_pidController.onTarget() == true) {
				ordreAuto = 2;
				
			}else if(PID_timer.get() >= 0.9) {
				ordreAuto = 2;
				PID_timer.stop();
				PID_timer.reset();
			}
			
		//PID_timer.get() >= 0.8 && roue < 4.75
		
			}else if (retourAuto == false  && ordreAuto == 2){
				if(autoLock2 == false) {
				
					ahrs.reset();
					PID_timer.stop();
					r_pidController.disable();
					l_pidController.disable();
					SmartDashboard.putString("PidDisabled","Yes");
					
					autoLock2 = true;
					autoLock1 = false;
			}
				
				double turningValue = -ahrs.getAngle() ;
				SmartDashboard.putNumber("Turning Value", turningValue);
				
				if(turningValue > 0.5) {
					turningValue = 0.5;
				}
				if (turningValue < -0.5) {
					turningValue = -0.5;
				}
			
				m_robotDrive.arcadeDrive(0, -turningValue * 0.7);
				
				if(0 == 0) {
					ordreAuto = 3;
				}
			}else if (ordreAuto == 3 || retourAuto == true ){
				
				retourAuto = true;
		
		if(limitswitchPince.get() == true && autoPince == false) {
					
					double[] gripValuesX = new double[0];
					double[] XValue =  xCenterEntry.getDoubleArray(gripValuesX);
					double angleVise = 320 / 25; // /10.33

				
					
					
					MPC1.set(0.45);
				MPC2.set(-0.45);
					
					if(XValue.length != 0) {

							if(premiereMesure == false) {
								encGauche.reset();
								encDroit.reset();
								ahrs.reset();
								double trouve =  XValue[0] / 25; //10.33
								SmartDashboard.putNumber("Trouve", trouve);
								

								
						
								premiereMesure = true;  
								
								calcul = trouve - angleVise;
								SmartDashboard.putNumber("Calculs", calcul);
							}
							
							if(premiereMesure == true) {
								
								double angleGyro = ahrs.getAngle();
								SmartDashboard.putNumber("Angle Gyro", angleGyro);
								double setPoint =  calcul - angleGyro ;
								SmartDashboard.putNumber("Set Point", setPoint);
								
								double setPointTrue = -setPoint * 0.8;
								if(setPointTrue > 0.52) {
									setPointTrue = 0.52;
								}
								if (setPointTrue < -0.52) {
									setPointTrue = -0.52;
								}
								m_robotDrive.arcadeDrive(-0.5, setPointTrue); //0.025
							}
                                                                                         
					}else if (premiereMesure == true) {
						double angleGyro = ahrs.getAngle();
						SmartDashboard.putNumber("Angle Gyro v2", angleGyro);
						double setPoint =  calcul - angleGyro ;
						SmartDashboard.putNumber("Set Point v2", setPoint);
						double setPointTrue = -setPoint * 0.8;
						if(setPointTrue > 0.52) {
							setPointTrue = 0.52;
						}
						if (setPointTrue < -0.52) {
							setPointTrue = -0.52;
						}
						m_robotDrive.arcadeDrive(-0.5,	setPointTrue); //0.025
			
					}else {
						m_robotDrive.arcadeDrive(0, -0.4);
				}
					//roue < 0.3
				}else if( ordreAuto == 3) {
					MPC1.set(0.15);
					MPC2.set(-0.15);
					autoPince = true;

	 			
					double turningValue = ahrs.getAngle();
				
					if(turningValue > 0.5) {
						turningValue = 0.5;
					}
					if (turningValue < -0.5) {
						turningValue = -0.5;
					}
	 			
					m_robotDrive.arcadeDrive(0.6, turningValue );
	 			
					if(roue >= 0) {
						ordreAuto = 4;
					}
				} else if (ordreAuto == 4) {
					MPC1.set(0.55);
					MPC2.set(-0.55);
					if(autoLock1 == false) {
						ahrs.reset();
						PID_timer.reset();
						PID_timer.start();
						
						
						retourTimer.start();
						r_pidController.enable();
						l_pidController.enable();
						r_pidController.setSetpoint(50);
						l_pidController.setSetpoint(50);
						
						r_pidController.setAbsoluteTolerance(0.001);
						l_pidController.setAbsoluteTolerance(0.001);
						autoLock1 = true;
						autoLock2 = false;
					}
					
					if(r_pidController.onTarget() == true && l_pidController.onTarget() == true) {
						ordreAuto = 5;
					}
					if(PID_timer.get() >= 0.9) {
						ordreAuto = 5;
					}
				} else if (ordreAuto == 5) {
		 			MPC1.set(0.15);
		 			MPC2.set(-0.15);
		 			r_pidController.disable();
		 			l_pidController.disable();
					double turningValue = -ahrs.getAngle();
					SmartDashboard.putNumber("Turning Value", turningValue);
					
					if(turningValue > 0.3) {
						turningValue = 0.3;
					}
					if (turningValue < -0.3) {
						turningValue = -0.3;
					}
					m_robotDrive.arcadeDrive(-0.75, 0);
					
					if(roue <= -10.4) {
						ordreAuto = 6;
					}
					//retourTimer.get() < 3.5
		 		} else if (ordreAuto == 6) {
					MPC1.set(-0.75);
					MPC2.set(0.75);
					
					if(retourTimer.get() >= 0.5) {
						ordreAuto = 7;
					}
		 		}else if(ordreAuto == 7) {
		 			if(autoLock2 == false) {
		 				ahrs.reset();
		 				retourTimer.reset();
		 				retourTimer.start();
		 		
		 				
		 				autoLock1 = false;
		 				autoLock2 = true;
		 			}
		 			MPC1.set(0);
		 			MPC2.set(0);
		 			
		 		
		 			
		 			
		 				ordreAuto = 12;
		 			
		 	
		 		}else	if(ordreAuto == 12) {
		 			if(autoLock1 == false) {
		 				ahrs.reset();
		 				encDroit.reset();
		 				encGauche.reset();
		 				
		 				autoLock2 = false;
		 				autoLock1 = true;
		 			}
		 			double turningValue = -ahrs.getAngle();
					if(turningValue > 0.3) {
						turningValue = 0.3;
					}
					if (turningValue < -0.3) {
						turningValue = -0.3;
					}
		 			
		 		 	m_robotDrive.arcadeDrive(0.6, 0);
		 		 		if(roue >= 8) {
		 		 			ordreAuto = 13;
		 		 		}
				} else if(ordreAuto == 13) {
					m_robotDrive.arcadeDrive(0, 0);
				}
	 				
		 		
		 		
		 		
				SmartDashboard.putNumber("ordreAuto", ordreAuto);
		}	
			
			break;
			
			case kCustomAuto6:
			
				//--------------------------Variables auto 1-----------------------------------
				countGauche = encGauche.get();
				countDroit = encDroit.get();
				roueGauche = -countGauche/240;
				 roueDroit = -countDroit/240;
				rouefalse = roueGauche - roueDroit;
			roue = rouefalse / 2;
		//480 = un tour
				SmartDashboard.putNumber("Tours", roue);
		
		
	//----------------------Debut mode autonome 1--------------------------
				if (retourAuto == false && ordreAuto == 0) {
					double turningValue = -ahrs.getAngle();
					SmartDashboard.putNumber("Turning Value", turningValue);

					if(turningValue > 0.3) {
						turningValue = 0.3;
					}
					
					else if (turningValue < -0.3) {
						turningValue = -0.3;
					}

					m_robotDrive.arcadeDrive(0.65, -turningValue * 0.7);	
				
				
					pneuPince.set(DoubleSolenoid.Value.kForward);
					encPince_pidController.enable();
					encPince_pidController.setSetpoint(700);
					
					if (roue >= 13.1 ) {  //12
					ordreAuto = 1;
				}
				
		//PID_t	imer.get() < 0.8
				}else if (retourAuto == false && ordreAuto == 1 ){
					if(autoLock1 == false) {
					//PID_timer.start();
					ahrs.reset();
					encGauche.reset();
					encDroit.reset();
					PID_timer.start();
					r_pidController.enable();
					l_pidController.enable();
					r_pidController.setSetpoint(48);
					l_pidController.setSetpoint(48);
					r_pidController.setOutputRange(-0.75, 0.75);
					l_pidController.setOutputRange(-0.75, 0.75);
					r_pidController.setAbsoluteTolerance(0.001);
					l_pidController.setAbsoluteTolerance(0.001);
					autoLock1 = true;
					autoLock2 = false;
				}
				SmartDashboard.putNumber("PId gyro value", ahrs.getAngle());
				if(r_pidController.onTarget() == true && l_pidController.onTarget() == true) {
					ordreAuto = 2;
					
				}else if(PID_timer.get() >= 0.9) {
					ordreAuto = 2;
					PID_timer.stop();
					PID_timer.reset();
				}
				
			//PID_timer.get() >= 0.8 && roue < 4.75
			
				}else if (retourAuto == false  && ordreAuto == 2){
					if(autoLock2 == false) {
					
						ahrs.reset();
						PID_timer.stop();
						r_pidController.disable();
						l_pidController.disable();
						SmartDashboard.putString("PidDisabled","Yes");
						
						autoLock2 = true;
						autoLock1 = false;
				}
					
					double turningValue = -ahrs.getAngle() ;
					SmartDashboard.putNumber("Turning Value", turningValue);
					
					if(turningValue > 0.5) {
						turningValue = 0.5;
					}
					if (turningValue < -0.5) {
						turningValue = -0.5;
					}
				
					m_robotDrive.arcadeDrive(0, -turningValue * 0.7);
					
					if(0 == 0) {
						ordreAuto = 3;
					}
				}else if (ordreAuto == 3 || retourAuto == true ){
					
					retourAuto = true;
			
			if(limitswitchPince.get() == true && autoPince == false) {
						
						double[] gripValuesX = new double[0];
						double[] XValue =  xCenterEntry.getDoubleArray(gripValuesX);
						double angleVise = 320 / 25; // /10.33

					
						
						
						MPC1.set(0.45);
					MPC2.set(-0.45);
						
						if(XValue.length != 0) {

								if(premiereMesure == false) {
									encGauche.reset();
									encDroit.reset();
									ahrs.reset();
									double trouve =  XValue[0] / 25; //10.33
									SmartDashboard.putNumber("Trouve", trouve);
									

									
							
									premiereMesure = true;  
									
									calcul = trouve - angleVise;
									SmartDashboard.putNumber("Calculs", calcul);
								}
								
								if(premiereMesure == true) {
									
									double angleGyro = ahrs.getAngle();
									SmartDashboard.putNumber("Angle Gyro", angleGyro);
									double setPoint =  calcul - angleGyro ;
									SmartDashboard.putNumber("Set Point", setPoint);
									
									double setPointTrue = -setPoint * 0.8;
									if(setPointTrue > 0.52) {
										setPointTrue = 0.52;
									}
									if (setPointTrue < -0.52) {
										setPointTrue = -0.52;
									}
									m_robotDrive.arcadeDrive(-0.5, setPointTrue); //0.025
								}
	                                                                                         
						}else if (premiereMesure == true) {
							double angleGyro = ahrs.getAngle();
							SmartDashboard.putNumber("Angle Gyro v2", angleGyro);
							double setPoint =  calcul - angleGyro ;
							SmartDashboard.putNumber("Set Point v2", setPoint);
							double setPointTrue = -setPoint * 0.8;
							if(setPointTrue > 0.52) {
								setPointTrue = 0.52;
							}
							if (setPointTrue < -0.52) {
								setPointTrue = -0.52;
							}
							m_robotDrive.arcadeDrive(-0.5,	setPointTrue); //0.025
				
						}else {
							m_robotDrive.arcadeDrive(0, -0.4);
					}
						//roue < 0.3
					}else if( ordreAuto == 3) {
						MPC1.set(0.15);
						MPC2.set(-0.15);
						autoPince = true;

		 			
						double turningValue = ahrs.getAngle();
					
						if(turningValue > 0.5) {
							turningValue = 0.5;
						}
						if (turningValue < -0.5) {
							turningValue = -0.5;
						}
		 			
						m_robotDrive.arcadeDrive(0.6, turningValue );
		 			
						if(roue >= -0.6) {
							ordreAuto = 4;
						}
					} else if (ordreAuto == 4) {
						MPC1.set(0.55);
						MPC2.set(-0.55);
						if(autoLock1 == false) {
							ahrs.reset();
							PID_timer.reset();
							PID_timer.start();
							
							
							retourTimer.start();
							r_pidController.enable();
							l_pidController.enable();
							r_pidController.setSetpoint(-42);
							l_pidController.setSetpoint(-42);
							
							r_pidController.setAbsoluteTolerance(0.05);
							l_pidController.setAbsoluteTolerance(0.05);
							autoLock1 = true;
							autoLock2 = false;
						}
						
						if(r_pidController.onTarget() == true && l_pidController.onTarget() == true) {
							ordreAuto = 5;
						}
						if(PID_timer.get() >= 0.9) {
							ordreAuto = 5;
						}
					} else if (ordreAuto == 5) {
			 			MPC1.set(0.15);
			 			MPC2.set(-0.15);
			 			r_pidController.disable();
			 			l_pidController.disable();
						double turningValue = -ahrs.getAngle();
						SmartDashboard.putNumber("Turning Value", turningValue);
						
						if(turningValue > 0.3) {
							turningValue = 0.3;
						}
						if (turningValue < -0.3) {
							turningValue = -0.3;
						}
						m_robotDrive.arcadeDrive(-0.75, 0);
						
						if(roue <= -10.4) {
							ordreAuto = 6;
						}
						//retourTimer.get() < 3.5
			 		} else if (ordreAuto == 6) {
						MPC1.set(-0.75);
						MPC2.set(0.75);
						
						if(retourTimer.get() >= 0.5) {
							ordreAuto = 7;
						}
			 		}else if(ordreAuto == 7) {
			 			if(autoLock2 == false) {
			 				ahrs.reset();
			 				retourTimer.reset();
			 				retourTimer.start();
			 		
			 				
			 				autoLock1 = false;
			 				autoLock2 = true;
			 			}
			 			MPC1.set(0);
			 			MPC2.set(0);
			 			
			 		
			 			
			 			
			 				ordreAuto = 12;
			 			
			 	
			 		}else	if(ordreAuto == 12) {
			 			if(autoLock1 == false) {
			 				ahrs.reset();
			 				encDroit.reset();
			 				encGauche.reset();
			 				
			 				autoLock2 = false;
			 				autoLock1 = true;
			 			}
			 			double turningValue = -ahrs.getAngle();
						if(turningValue > 0.3) {
							turningValue = 0.3;
						}
						if (turningValue < -0.3) {
							turningValue = -0.3;
						}
			 			
			 		 	m_robotDrive.arcadeDrive(0.6, 0);
			 		 		if(roue >= 8) {
			 		 			ordreAuto = 13;
			 		 		}
					} else if(ordreAuto == 13) {
						m_robotDrive.arcadeDrive(0, 0);
					}
		 				
			 		
			 		
			 		
					SmartDashboard.putNumber("ordreAuto", ordreAuto);
			}	
		break;
			
			
		
		case kDefaultAuto:
		default:
			pneuPince.set(DoubleSolenoid.Value.kForward);
			encPince_pidController.enable();
			encPince_pidController.setSetpoint(700);
			float EncoderPince = encPince.get();
			SmartDashboard.putNumber("Amazing", EncoderPince);
			break;

	}}
		
			

				 			  
			
			
//Fin du mode autonome 
	//	}
		

//--------------------------Initialisation du mode teleopere--------------------------------
	@Override
	public void teleopInit() {

	//On active nos trucs
		encGauche.reset();
		limit_timer.reset();
		limit_timer.start();
		rotationPelle = 1;
		pneuPince.set(DoubleSolenoid.Value.kOff);
		pneuPelle.set(DoubleSolenoid.Value.kOff);
		
		r_pidController.enable();
		r_pidController.setSetpoint(0);
		l_pidController.enable();
		l_pidController.setSetpoint(0);
		l_pidController.setOutputRange(-1, 1);
		r_pidController.setOutputRange(-1, 1);
	//	encPince_pidController.enable();
	//	encPince_pidController.setSetpoint(124);
		
		rotationPelle90Moins = false;
		rotationPelle180Moins = false;
		rotationPelle180Plus = false;
		rotationPelle90Plus = false;
	

//Fin de teleopInit
	}

//----------------------------Deroulement du mode Teleop------------------------------------
	@Override
	public void teleopPeriodic() { 
		
//Network Tables
	//C'est la qu'on va mettre nos Network Tables, si besoin est	
//SmartDashboard.putBoolean("La Vie", c.enabled());
	


	//	if(coPilote_stick.getRawButtonPressed(10)==true) {
//Pelle vers le bas
		/*	if(rotationPelle == 2) {
				//+90
				rotationPelle90Plus = true;
				rotationPelle90Moins = false;
				rotationPelle180Moins = false;
				rotationPelle180Plus = false;
				
				pelleTimer.reset();
				pelleTimer.start();

			}
			else if(rotationPelle == 3) {
				rotationPelle180Plus = false;		
				rotationPelle90Moins = true;
				rotationPelle180Moins = false;
				rotationPelle90Plus = false;
				
				pelleTimer.reset();
				pelleTimer.start();
			}
				
			
			if 
			
			
			rotationPelle = 1;
			*///}
			
			
			
			if(coPilote_stick.getRawButton(9)==true) {
//Vers l'interieur
				MPelle1.set(-0.6);
				
		/*	if(rotationPelle == 1) {
					rotationPelle90Moins = true;
					rotationPelle180Moins = false;
					rotationPelle180Plus = false;
					rotationPelle90Plus = false;
					
					pelleTimer.reset();
					pelleTimer.start();
				}
				else if(rotationPelle == 3) {
					rotationPelle90Plus = false;
					
					rotationPelle90Moins = false;
					rotationPelle180Moins = true;
					rotationPelle180Plus = false;
				
					pelleTimer.reset();
					pelleTimer.start();
				}
			
				
				
				
			rotationPelle = 2;
		*/	}
			if(coPilote_stick.getRawButton(6)==true) {
//Vers l'exterieur
				MPelle1.set(0.6);
		/*	
				if(rotationPelle == 1) {
					rotationPelle180Moins = false;
					rotationPelle90Moins = false;
					rotationPelle180Plus = false;
					rotationPelle90Plus = true;
					
					pelleTimer.reset();
					pelleTimer.start();
				}
				else if(rotationPelle == 2) {
					rotationPelle90Moins = false;
					rotationPelle180Moins = false;
					rotationPelle180Plus = true;
					rotationPelle90Plus = false;
					
					pelleTimer.reset();
					pelleTimer.start();
				}
				
				
			rotationPelle = 3;
		*/
			}
			
			if(coPilote_stick.getRawButton(9) == false && coPilote_stick.getRawButton(6) == false) {
				MPelle1.set(0);
			}
		
			if(coPilote_stick.getRawButtonPressed(8)==true) {
	//Avancer reculer pince
				
				if(pelleAvance == false) {
					
					pneuPelle.set(DoubleSolenoid.Value.kForward);
				//	pneuPince.set(DoubleSolenoid.Value.kReverse);
					
				} else if (pelleAvance == true) {
					
					pneuPelle.set(DoubleSolenoid.Value.kReverse);
				//	pneuPince.set(DoubleSolenoid.Value.kForward);
				
				}
				
				

				pelleAvance = !pelleAvance;
			}
			
			if(coPilote_stick.getRawButtonPressed(10)==true) {
				//Avancer reculer pince
							
							if(pelleHaut == false) {
								
								pneuPince.set(DoubleSolenoid.Value.kForward);
							//	pneuPince.set(DoubleSolenoid.Value.kReverse);
								
							} else if (pelleHaut == true) {
								
								pneuPince.set(DoubleSolenoid.Value.kReverse);
							//	pneuPince.set(DoubleSolenoid.Value.kForward);
							
							}
							
							
							pelleHaut = !pelleHaut;
						}
		
		
		
	/*	if(rotationPelle90Plus == true) {
			if(pelleTimer.get() <= 0.3) {
				MPelle1.set(0.6);
		} else {
			MPelle1.set(0);
			rotationPelle90Plus = false;
		}
			
		}
		if(rotationPelle90Moins == true) {
			if(pelleTimer.get() <= 0.3) {
				MPelle1.set(-0.6);
			}else{
				MPelle1.set(0);
				rotationPelle90Moins = false;
			}
			
	
		}
		if(rotationPelle180Plus == true) {
		 if(pelleTimer.get() <= 0.6) {
			 MPelle1.set(0.6); 
		 }else{
				MPelle1.set(0);
				rotationPelle180Plus = false;
					
			}
		}
		if(rotationPelle180Moins == true) {
			if(pelleTimer.get() < 0.6) {
			MPelle1.set(-0.6);
			}else{
				MPelle1.set(0);
				rotationPelle180Moins = false;
					
			}
			
		}
*/

	//Activer les roulettes de la pince(R1 et L1)
		
		if(coPilote_stick.getRawButton(1) == true) {
			MPC1.set(-0.6);
			MPC2.set(0.6);
		} else if(limitswitchPince.get() == true && coPilote_stick.getRawButton(2) == true)	{
		 
			MPC1.set(0.35);
			MPC2.set(-0.35);
	 
		}else {
			MPC1.set(0);
			MPC2.set(0);
		}
		
		//Deployer/controller la pince
		
			if(coPilote_stick.getRawButton(4) == true) {
				encPince_pidController.disable();
				MPR1.set(0.6);
			}else if (coPilote_stick.getRawButton(3)) {
				encPince_pidController.disable();
				MPR1.set(-0.6);
			}else {
				MPR1.set(0);
			}
			
//------------------------------------------Conduite-------------------------
		
		if(coPilote_stick.getRawButtonPressed(7)) {
			GripOk = !GripOk;
			YbutonTrigger = !YbutonTrigger;
			r_pidController.disable();
			l_pidController.disable();
			premiereMesure = false;
		}
	if(GripOk == true) {
		if(limitswitchPince.get() == true ) {
			
			double[] gripValuesX = new double[0];
			double[] XValue =  xCenterEntry.getDoubleArray(gripValuesX);
			double angleVise = 320 * 7 / 127; // /10.33

		
			
			
			MPC1.set(0.45);
		MPC2.set(-0.45);
			
			if(XValue.length != 0) {

					if(premiereMesure == false) {
						encGauche.reset();
						encDroit.reset();
						ahrs.reset();
						double trouve =  XValue[0] * 7 / 127; //10.33
						SmartDashboard.putNumber("Trouve", trouve);
						

						
				
						premiereMesure = true;  
						
						calcul = trouve - angleVise;
						SmartDashboard.putNumber("Calculs", calcul);
					}
					
					if(premiereMesure == true) {
						
						double angleGyro = ahrs.getAngle();
						SmartDashboard.putNumber("Angle Gyro", angleGyro);
						double setPoint =  calcul - angleGyro ;
						SmartDashboard.putNumber("Set Point", setPoint);
						
						double setPointTrue = -setPoint * 0.8;
						if(setPointTrue > 0.52) {
							setPointTrue = 0.52;
						}
						if (setPointTrue < -0.52) {
							setPointTrue = -0.52;
						}
						m_robotDrive.arcadeDrive(-0.5, setPointTrue); //0.025
					}
                                                                                 
			}else if (premiereMesure == true) {
				double angleGyro = ahrs.getAngle();
				SmartDashboard.putNumber("Angle Gyro v2", angleGyro);
				double setPoint =  calcul - angleGyro ;
				SmartDashboard.putNumber("Set Point v2", setPoint);
				double setPointTrue = -setPoint * 0.8;
				if(setPointTrue > 0.52) {
					setPointTrue = 0.52;
				}
				if (setPointTrue < -0.52) {
					setPointTrue = -0.52;
				}
				m_robotDrive.arcadeDrive(-0.5,	setPointTrue); //0.025
	
			}else {
				m_robotDrive.arcadeDrive(0, -0.4);
		}
			} else {
				GripOk = false;
				YbutonTrigger = false;
			}
	}
	
			
			
	//ici, on verifie si les boutons B et Y ne sont pas actionnes
		if(Bbuton == false && YbutonTrigger == false) {
		
		//Desactivation du pid
			r_pidController.disable();
			l_pidController.disable();
		
		//Creation de l'effet de toggle 
			if(m_stick.getRawButtonPressed(9) == true) {
				lent = !lent;
				}
			
			
		//Mode lent	
			if (lent == false){
				double ystick = m_stick.getRawAxis(2) * 02 - m_stick.getRawAxis(3) * 2;
				double xstick = - m_stick.getRawAxis(0) * 2;
				m_robotDrive.arcadeDrive(-ystick, xstick);
				}
			
		//Mode rapide
			else if (lent == true) {
				double ystick = m_stick.getRawAxis(2) * 0.6 - m_stick.getRawAxis(3) * 0.6;
				double xstick = - m_stick.getRawAxis(0) * 0.6;
				m_robotDrive.arcadeDrive(-ystick, xstick);
				}
			
			}
		
		//Les gyros
		double angle = ahrs.getAngle();
		double angleabs = angle;
		
		
		//----------------Calcul d'angle---------------------
		// De infinie vers -360 a 360
		if(angleabs > 0) {
			double a = angleabs / 360;
			int b = (int)a;
			double c = angleabs - (b * 360);
			if(c > 180) {
				c -= 360;
			}
			else if(c <= 180) {
				
			}
			angleabs = c;
		}
		else if(angleabs < 0) {
			double a = angleabs / 360;
			int b = (int)a;
			double c = angleabs - (b * 360);
			if(c < -180) {
				c += 360;
			}
			else if(c >= -180) {}
			angleabs = c;
		}
		else {}
		
		SmartDashboard.putNumber("Gyro angle", angle);
		SmartDashboard.putNumber("Gyro angle (ABS)", angleabs);
		
		//-----------Fin du calcul d'angle---------------
		
		
		//----------------A Button------------------------
		if( m_stick.getRawButtonPressed(1) == true){
			ahrs.reset();
			r_pidController.setSetpoint(0);
			l_pidController.setSetpoint(0);
		//	pneuPince.set(DoubleSolenoid.Value.kReverse);	
			}		
			

		//----------------------------B buton-----------------------------------
		//Garder B button enfonce pour qu'il finisse
		
		if(m_stick.getRawButton(2) == true && Bbuton == false) {
		//Activation du PID	
			double gyroRaw = ahrs.getRawGyroX();
			ahrs.reset();
			
			SmartDashboard.putNumber("Raw Gyro", gyroRaw);
			r_pidController.enable();
			l_pidController.enable();
			double obj = angle +180;
			r_pidController.setSetpoint(obj);
			l_pidController.setSetpoint(obj);
			Bbuton = true;
			
			SmartDashboard.putNumber("PID objectif", obj);
		//	pneuPince.set(DoubleSolenoid.Value.kForward);
			}
		
	//Eviter 3000 fois le meme bouton. S'active quand on lache le bouton
		if(m_stick.getRawButton(2) == false) {
			Bbuton = false;
			}
			
		
	
//Fin du teleop	
	}
	
	@Override
	public void testPeriodic() {
	}
}
