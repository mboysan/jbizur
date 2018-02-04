package processor;

import protocol.commands.GenericCommand;

public class CommandProcessor {

    public void processCommand(GenericCommand command){
        System.out.println("Processing command: " + command.toString());
    }

}
