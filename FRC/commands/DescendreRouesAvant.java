/*----------------------------------------------------------------------------*/
/* Copyright (c) 2018 FIRST. All Rights Reserved.                             */
/* Open Source Software - may be modified and shared by FRC teams. The code   */
/* must be accompanied by the FIRST BSD license file in the root directory of */
/* the project.                                                               */
/*----------------------------------------------------------------------------*/

package frc.robot.commands;

import edu.wpi.first.wpilibj.command.Command;
import frc.robot.Robot;

public class DescendreRouesAvant extends Command {
  public DescendreRouesAvant() {
  requires(Robot.lifterSubsystem);
  }

  @Override
  protected void initialize() {
  Robot.lifterSubsystem.descendreRouesAvant();
  Robot.RouesMonte.Coast();

  }

  @Override
  protected void execute() {
  
  }

  @Override
  protected boolean isFinished() {
    return false;
  }

  @Override
  protected void end() {
  }

  @Override
  protected void interrupted() {
    end();
  }
}
