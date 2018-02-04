package processor;

import protocol.commands.GenericCommand;
import protocol.commands.ICommand;

public class CommandProcessor {

    public void processCommand(ICommand command){
        if(command instanceof GenericCommand){
            System.out.println("Processing command: " + command.toString());
        } else {
            throw new UnsupportedOperationException("Unknown command class: " + command.getClass());
        }
    }

}
