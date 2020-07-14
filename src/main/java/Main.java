import twitch.io.FileCreator;
import twitch.Checker;
import twitch.FollowSender;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        FileCreator.create();
        Checker checker;
        FollowSender followSender;

        if (args.length > 2 || args.length == 0) {
            throw new IllegalArgumentException("Wrong arguments");
        }
        if (args[0].toLowerCase().equals("follow")) {
            if (args[1] == null) throw new IllegalArgumentException("Enter channel");
            if (args[1].toLowerCase().isEmpty()) throw new IllegalArgumentException("Wrong channel");
            followSender = new FollowSender(args[1]);
            followSender.start();
        } else if (args[0].toLowerCase().equals("check")) {
            checker = new Checker();
            checker.start();
        }
    }
}
