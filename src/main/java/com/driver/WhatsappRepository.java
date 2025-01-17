package com.driver;

import java.util.*;

import org.springframework.stereotype.Repository;

@Repository
public class WhatsappRepository {

    //Assume that each user belongs to at most one group
    //You can use the below mentioned hashmaps or delete these and create your own.
    private HashMap<Group, List<User>> groupUserMap;
    private HashMap<Group, List<Message>> groupMessageMap;
    private HashMap<Message, User> senderMap;
    private HashMap<Group, User> adminMap;
    private HashSet<String> userMobile;
    private int customGroupCount;
    private int messageId;

    public WhatsappRepository(){
        this.groupMessageMap = new HashMap<Group, List<Message>>();
        this.groupUserMap = new HashMap<Group, List<User>>();
        this.senderMap = new HashMap<Message, User>();
        this.adminMap = new HashMap<Group, User>();
        this.userMobile = new HashSet<>();
        this.customGroupCount = 0;
        this.messageId = 0;
    }

    public String createUser(String name, String mobile) {

        if (userMobile.contains(mobile))
        {
            throw new RuntimeException("User already exists");
        }

        User user = new User(name, mobile);
        userMobile.add(mobile);
        return "SUCCESS";
    }

    public Group createGroup(List<User> users) {

        if (users.size() < 2)
        {
            throw new RuntimeException("At least 2 users are required to create a group.");
        }

        Group group = null;
        if (users.size() == 2)
        {
            group = new Group(users.get(0).getName(), users.size());
        }
        else
        {
            group = new Group("Group " + (++customGroupCount), users.size());
        }

        groupUserMap.put(group, users);
        groupMessageMap.put(group, new ArrayList<>());

        adminMap.put(group, users.get(0));

        return group;
    }

    public int createMessage(String content) {

        Message message = new Message((++messageId), content, new Date());

        return messageId;
    }


    public int sendMessage(Message message, User sender, Group group) {

        if (!groupUserMap.containsKey(group))
        {
            throw new RuntimeException("Group does not exist");
        }

        List<User> groupUsers = new ArrayList<>();
        groupUsers = groupUserMap.get(group);

        if (!groupUsers.contains(sender))
        {
            throw new RuntimeException("You are not allowed to send message");
        }

        groupMessageMap.get(group).add(message);
        senderMap.put(message, sender);
        return groupMessageMap.get(group).size();

    }

    public String changeAdmin(User approver, User user, Group group) {

        if (!groupUserMap.containsKey(group))
        {
            throw new RuntimeException("Group does not exist");
        }

        if (adminMap.get(group) != approver)
        {
            throw new RuntimeException("Approver does have rights");
        }

        if (!groupUserMap.get(group).contains(user))
        {
            throw new RuntimeException("User is not a participant");
        }

        adminMap.put(group, user);

        return "SUCCESS";
    }

    public int removeUser(User user) {

        Group groupToRemove = null;
        for (Map.Entry<Group, List<User>> entry : groupUserMap.entrySet())
        {
            if (entry.getValue().contains(user))
            {
                groupToRemove = entry.getKey();
                break;
            }
        }

        if (groupToRemove == null)
        {
            throw new RuntimeException("User not found");
        }

        if (adminMap.get(groupToRemove) == user)
        {
            throw new RuntimeException(("Cannot remove admin"));
        }

        // Remove the user from the group
        List<User> usersInGroup = groupUserMap.get(groupToRemove);
        usersInGroup.remove(user);

        // Remove all messages sent by the user from the group's message list
        List<Message> messagesInGroup = groupMessageMap.get(groupToRemove);
        messagesInGroup.removeIf(message -> senderMap.get(message).equals(user));

        // Remove the user's messages from the sender map
        senderMap.entrySet().removeIf(entry -> entry.getValue().equals(user));

        // Calculate the updated total number of messages across all groups
        int totalMessagesAcrossAllGroups = groupMessageMap.values().stream().mapToInt(List::size).sum();

        return usersInGroup.size() + messagesInGroup.size() + totalMessagesAcrossAllGroups;

    }

    public String findMessage(Date start, Date end, int k) {

        List<Message> messagesInRange = new ArrayList<>();
        for (Map.Entry<Group, List<Message>> entry : groupMessageMap.entrySet())
        {
            for (Message message : entry.getValue())
            {
                if (message.getTimestamp().after(start) && message.getTimestamp().before(end))
                {
                    messagesInRange.add(message);
                }
            }
        }

        if (messagesInRange.size() < k)
        {
            throw new RuntimeException("K is greater than the number of messages");
        }

        messagesInRange.sort(Comparator.comparing(Message::getTimestamp).reversed());

        return messagesInRange.get(k - 1).getContent();
    }
}
