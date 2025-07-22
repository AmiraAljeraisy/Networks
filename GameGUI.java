
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class GameGUI extends JFrame {
    private CardLayout cardLayout;
    private JPanel mainPanel;
    private JTextArea playerListArea, waitingListArea;
    private JTextArea scoreArea, timerArea; 
    private JLabel currentWord, winnerArea;
    private String playerName, ANS;
    private int timeRemaining = 30;  
    private Timer gameTimer;
    private PrintWriter out;
    private BufferedReader in;
    private Socket socket;
    private ArrayList<String> connectedPlayers = new ArrayList<>();
    private ArrayList<String> waitingPlayers = new ArrayList<>();
    private ArrayList<String> gameRoom = new ArrayList<>(); // to track players in the game room
    private final int MAX_PLAYERS = 3; // max num of player
    private static boolean winner = true ,left = true;
    
    public GameGUI() {
        try {
            setTitle("Game Interface");
            setSize(522, 528);
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setLocationRelativeTo(null); 

            
            cardLayout = new CardLayout();
            mainPanel = new JPanel(cardLayout);

            
            addPage1();
            addPage2();
            addPage3();
            addPage4();
            addPage5();
            addPage6();
            
            getContentPane().add(mainPanel);
            setVisible(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Page 1: interfece
    private void addPage1() {
        new Thread(() -> welcome.playSound("intro-logo-13488.wav")).start();
        JPanel page1 = new JPanel();
        JLabel welcome = new JLabel("مرحباً بك نتمنى لك تجربة مميزة!", JLabel.CENTER);
        welcome.setFont(new Font("Arabic Typesetting", Font.BOLD | Font.ITALIC, 30));
        welcome.setForeground(new Color(53, 0, 130));
        welcome.setBounds(35, 164, 433, 95);
        JButton startButton = new JButton("بدأ");
        startButton.setForeground(new Color(53, 0, 130));
        startButton.setFont(new Font("Arabic Typesetting", Font.BOLD, 26));
        startButton.setBounds(189, 303, 120, 30);
        startButton.setPreferredSize(new Dimension(100, 50)); 
        startButton.addActionListener(e -> cardLayout.show(mainPanel, "Page 2"));
        page1.setLayout(null);

        page1.add(welcome);
        page1.add(startButton);
        mainPanel.add(page1, "Page 1");
        
        JLabel background = new JLabel("background");
        background.setIcon(new ImageIcon("background.jpg"));
        background.setBounds(0, 0, 514, 491);
        page1.add(background);
    }

    // Page 2: Input name and Connect
    private void addPage2() {
        JPanel page2 = new JPanel();
        
        JLabel enterName = new JLabel(" لطفاً، يرجى كتابة اسمك: ");
        enterName.setForeground(new Color(53, 0, 130));
        enterName.setBounds(131, 145, 234, 67);
        enterName.setFont(new Font("Arabic Typesetting", Font.BOLD | Font.ITALIC, 30));
        enterName.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        
        JTextField nameField = new JTextField();
        nameField.setBounds(141, 225, 230, 40);
        nameField.setFont(new Font("Arabic Typesetting", Font.BOLD, 24));
        nameField.setForeground(new Color(53, 0, 130));
        nameField.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        
        JButton connectButton = new JButton("اتصال"); 
        connectButton.setForeground(new Color(53, 0, 130));

        connectButton.setFont(new Font("Arabic Typesetting", Font.BOLD, 26));
        connectButton.setBounds(189, 303, 120, 30);
        connectButton.setPreferredSize(new Dimension(100, 30));
        connectButton.addActionListener(e -> {
            playerName = nameField.getText();

            if (!(playerName == null || playerName.isEmpty()) && playerName.matches("[a-zA-Z\\u0600-\\u06FF]+")) {
                connectToServer(playerName);
                new Thread(() -> welcome.playSound("new-notification-7-210334.wav")).start();
                JOptionPane.showMessageDialog(this, playerName + " متصله!");// pop up
                cardLayout.show(mainPanel, "Page 3");
            } else {
                JOptionPane.showMessageDialog(this, "الرجاء إدخال اسم صالح.");// pop up
            }
        });
        page2.setLayout(null);

        page2.add(enterName);
        page2.add(nameField);
        page2.add(connectButton);
        mainPanel.add(page2, "Page 2");

        JLabel background = new JLabel("background");
        background.setIcon(new ImageIcon("background.jpg"));
        background.setBounds(0, 0, 508, 491);
        page2.add(background);
    }

    // Page 3: List of connected players and play button
    private void addPage3() {
        JPanel page3 = new JPanel();
        JLabel connectPlayer = new JLabel("اللاعبين المتصلين");
        connectPlayer.setForeground(new Color(53, 0, 130));
        connectPlayer.setBounds(135, 90, 234, 67);
        connectPlayer.setFont(new Font("Arabic Typesetting", Font.BOLD | Font.ITALIC, 30));
        connectPlayer.setHorizontalAlignment(SwingConstants.CENTER);

        JButton playButton = new JButton("لعب");
        playButton.setForeground(new Color(53, 0, 130));
        playButton.setFont(new Font("Arabic Typesetting", Font.BOLD, 26));
        playButton.setBounds(189, 303, 120, 30);
        playButton.setPreferredSize(new Dimension(100, 30)); 
        
        playButton.addActionListener(e -> {
           
        	out.println("لعب");  
            out.flush(); // Ensure data is sent immediately
       });
        
       
  
        page3.setLayout(null);

        page3.add(connectPlayer);

        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setBounds(52, 164, 400, 130);
        page3.add(scrollPane);

        playerListArea = new JTextArea();
        scrollPane.setViewportView(playerListArea);
        playerListArea.setFont(new Font("Arabic Typesetting", Font.BOLD, 24));
        playerListArea.setForeground(new Color(53, 0, 130));
        playerListArea.setEditable(false);
        playerListArea.setLineWrap(true);
        playerListArea.setWrapStyleWord(true);
        playerListArea.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT); 

        
        page3.add(playButton);
        mainPanel.add(page3, "Page 3");

        JLabel background = new JLabel("background");
        background.setIcon(new ImageIcon("background.jpg"));
        background.setBounds(0, 0, 508, 491);
        page3.add(background);
    }

    // Page 4: Waiting room with list of players in the Waiting
    private void addPage4() {
        JPanel page4 = new JPanel();

        JLabel WaitingRoom = new JLabel("غرفة الانتظار");
        WaitingRoom.setForeground(new Color(53, 0, 130));
        WaitingRoom.setBounds(135, 90, 234, 67);
        WaitingRoom.setFont(new Font("Arabic Typesetting", Font.BOLD | Font.ITALIC, 30));
        WaitingRoom.setHorizontalAlignment(SwingConstants.CENTER);
        
        page4.setLayout(null);
        page4.add(WaitingRoom);


        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setBounds(52, 160, 400, 160);
        page4.add(scrollPane);

        waitingListArea = new JTextArea();
        scrollPane.setViewportView(waitingListArea);
        waitingListArea.setFont(new Font("Arabic Typesetting", Font.BOLD, 24));
        waitingListArea.setForeground(new Color(53, 0, 130));
        waitingListArea.setEditable(false);
        waitingListArea.setLineWrap(true);
        waitingListArea.setWrapStyleWord(true);
        waitingListArea.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT); 

      
        mainPanel.add(page4, "Page 4");

        JLabel background = new JLabel("background");
        background.setIcon(new ImageIcon("background.jpg"));
        background.setBounds(0, 0, 508, 491);
        page4.add(background);
    }

 // Page 5: Game Room with Player Scores   
    private void addPage5() {
        JPanel page5 = new JPanel();
        page5.setBackground(new Color(255, 255, 255));
        
        currentWord = new JLabel(); 
        currentWord.setForeground(new Color(53, 0, 130));
        currentWord.setBackground(new Color(0, 0, 0, 0));
        currentWord.setOpaque(false); 
        currentWord.setBounds(163, 102, 170, 50);
        currentWord.setFont(new Font("Arial", Font.BOLD, 36));
        currentWord.setHorizontalAlignment(SwingConstants.CENTER);
        currentWord.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        
        JTextField ANSField= new JTextField();
        ANSField.setBounds(140, 200, 220, 50);
        ANSField.setFont(new Font("Arabic Typesetting", Font.BOLD, 24));
        ANSField.setForeground(new Color(53, 0, 130));
        ANSField.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        
        JButton sendButton = new JButton("إرسال");
        sendButton.setForeground(new Color(53, 0, 130));
        sendButton.setFont(new Font("Arabic Typesetting", Font.BOLD, 26));
        sendButton.setBounds(185, 300, 120, 30);
        sendButton.setPreferredSize(new Dimension(100, 30)); 
        sendButton.addActionListener(e -> {
        	
            if (out != null) {
                ANS = ANSField.getText().trim();
                
                if (!ANS.isEmpty()) {
                    out.println("synonym: " + ANS); 
                    
                    out.flush();
                    ANSField.setText("");
                  
                    
                } else {
                    JOptionPane.showMessageDialog(this, "الرجاء إدخال إجابة قبل الإرسال.");
                }
            }
        });
        
        JButton leaveButton = new JButton("مغادرة");
        leaveButton.setForeground(new Color(53, 0, 130));
        leaveButton.setFont(new Font("Arabic Typesetting", Font.BOLD, 26));
        leaveButton.setBounds(185, 350, 120, 30);
        leaveButton.setPreferredSize(new Dimension(100, 30));
        leaveButton.addActionListener(e -> {
            if (out != null) {
                out.println("مغادرة");
                out.flush();
                JOptionPane.showMessageDialog(this, "لقد تركت اللعبة.");
                System.exit(0); 
            }
        });
      
        // Add score area for displaying player scores
        JLabel scoreLabel = new JLabel("نقاط :");
        scoreLabel.setForeground(new Color(204, 204, 255));
        scoreLabel.setFont(new Font("Arabic Typesetting", Font.BOLD, 16));  
        scoreLabel.setBounds(468, 5, 55, 30);
        
        
        scoreArea = new JTextArea(); 
        scoreArea.setForeground(new Color(204, 204, 255));
        scoreArea.setBackground(new Color(0, 0, 0, 0));
        scoreArea.setOpaque(false); 
        scoreArea.setBounds(400, 12, 100, 86);
        scoreArea.setEditable(false);
        scoreArea.setFont(new Font("Arabic Typesetting", Font.BOLD, 20));
        scoreArea.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        
     // Timer area
        timerArea = new JTextArea("مؤقت :" + timeRemaining); 
        timerArea.setForeground(new Color(204, 204, 255));
        timerArea.setBackground(new Color(255, 255, 255));
        timerArea.setOpaque(false);
        timerArea.setBounds(3, 5, 70, 30);  
        timerArea.setEditable(false);
        timerArea.setFont(new Font("Arabic Typesetting", Font.BOLD, 16));
        timerArea.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
          
        page5.setLayout(null);
        
        page5.add(scoreLabel);
        page5.add(scoreArea);
        page5.add(timerArea);
        page5.add(currentWord);
        page5.add(ANSField);
        page5.add(sendButton);
        page5.add(leaveButton);
        mainPanel.add(page5, "Page 5");

        JLabel background = new JLabel("background");
        background.setIcon(new ImageIcon("background.jpg"));
        background.setBounds(0, 0, 508, 491);
        page5.add(background);
    }
    
    
 // Page 6: End Game with Winner Player   
    private void addPage6() {
        JPanel page6 = new JPanel();
        
        JLabel winner = new JLabel("اللاعب الفائز");
        winner.setForeground(new Color(53, 0, 130));
        winner.setBounds(110, 138, 279, 67);
        winner.setFont(new Font("Arabic Typesetting", Font.BOLD | Font.ITALIC, 40));
        winner.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        winner.setHorizontalAlignment(SwingConstants.CENTER);
        
        winnerArea = new JLabel();  
        winnerArea.setForeground(new Color(53, 0, 130));
        winnerArea.setOpaque(false); 
        winnerArea.setBounds(126, 227, 242, 67);
        winnerArea.setFont(new Font("Arabic Typesetting", Font.BOLD | Font.ITALIC, 30));
        winnerArea.setHorizontalAlignment(SwingConstants.CENTER);
        
        page6.setLayout(null);
        
        
        page6.add(winner);
        page6.add(winnerArea);

        
        mainPanel.add(page6, "Page 6");

        JLabel background = new JLabel("background");
        background.setIcon(new ImageIcon("background.jpg"));
        background.setBounds(0, 0, 508, 491);
        page6.add(background);
    
    }
    
    // Connect to the server and handle player registration
    private void connectToServer(String playerName) {
        try {
            socket = new Socket("192.168.8.128", 7777);  

            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));

            // Send player name to the server
            out.println(playerName);

          
            new Thread(() -> {
                try {
                    String serverMessage;
                    while ((serverMessage = in.readLine()) != null) {
                    	handleServerMessage(serverMessage); 
                        if (serverMessage.startsWith("Connected players")) {
                            updateConnectedPlayers(serverMessage);
                        } else if (serverMessage.startsWith("Waiting list")) {
                            updateWaitingList(serverMessage);  
                    }
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
                
            }).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    
    private void handleServerMessage(String message) {
    	
        if (message.equals("Room is full. You cannot join the game.")) {
            JOptionPane.showMessageDialog(this, "الغرفة ممتلئة. لا يمكنك الانضمام إلى اللعبة.", "Room Full", JOptionPane.WARNING_MESSAGE);
        
        } else if (message.equals("You have joined the waiting Room.")) {
            JOptionPane.showMessageDialog(this, "لقد انضممت إلى غرفة الانتظار.", "Joined", JOptionPane.INFORMATION_MESSAGE);
            cardLayout.show(mainPanel, "Page 4");
       
        } else if (message.startsWith("Waiting list:")) {
            updateWaitingList(message);
            String[] players = message.replace("Waiting list: ", "").split(", ");
            if (players.length == MAX_PLAYERS) {
                cardLayout.show(mainPanel, "Page 5");
            }
            
        } else if (message.startsWith("Game starting")) {
            String[] players = message.replace("Game starting: ", "").split(", ");
            for (String player : players) {
                gameRoom.add(player);
            }
            startGameTimer();
            cardLayout.show(mainPanel, "Page 5");
         
        } else if (message.startsWith("Scores:")) {
            updateScores(message.replace("Scores:", "").trim());
           
        } else if (message.startsWith("Updated Scores: ")) {
            updateScores(message.replace("Updated Scores: ", "").trim());
         
        }else if (message.endsWith( "has left the game.")) {
            String playerLeft = message.replace("has left the game.", "").trim();
            if(left)
            for(String player : gameRoom) {
        	JOptionPane.showMessageDialog(this, playerLeft + " غادر اللعبة.", "has left", JOptionPane.INFORMATION_MESSAGE);
        	left = false;
            }
        }else if (message.startsWith("Connected players")) {
                updateConnectedPlayers(message);
              
        } else if (message.startsWith("Game ending as only one player remains")) {
            String remainingPlayer = message.replace("Game ending as only one player remains: ", "").trim();
            JOptionPane.showMessageDialog(this, "تهانينا " + remainingPlayer + "! أنت آخر لاعب في اللعبة.", "Game Over", JOptionPane.INFORMATION_MESSAGE);
            new Thread(() -> SoundPlayer.playSound("success-fanfare-trumpets-6185.wav")).start();
            Winner(remainingPlayer);
            gameTimer.stop();
            gameRoom.clear(); 
            cardLayout.show(mainPanel, "Page 6"); 
            
        
        } else if (message.startsWith("Word:")) {
        	 String word = message.substring(5).trim(); 
            updateCurrentWord(word);

        }else if (message.startsWith("Valid synonym!")) {
            new Thread(() -> Correct.playSound("correct-6033.wav")).start();
        JOptionPane.showMessageDialog(this, "مرادف صالح!", "Valid", JOptionPane.INFORMATION_MESSAGE);
        
        } else if (message.startsWith("Invalid synonym")) {
            new Thread(() -> wrong.playSound("cartoon-trombone-sound-effect-241387.wav")).start();
            JOptionPane.showMessageDialog(this, "مرادف غير صالح!", "Invalid", JOptionPane.WARNING_MESSAGE);
        
        }else if (message.startsWith("Game over! Winner: ") && winner) {
        	winner = false;
            String winner = message.replace("Game over! Winner: ", "").trim();
            JOptionPane.showMessageDialog(this, "انتهت اللعبة! " , "Game Over", JOptionPane.INFORMATION_MESSAGE);
            new Thread(() -> SoundPlayer.playSound("success-fanfare-trumpets-6185.wav")).start();
            Winner(winner);
            gameRoom.clear();
            cardLayout.show(mainPanel, "Page 6");
            
        } else if (message.startsWith("Game over! It's a tie! Winners: ") && winner) {
        	winner = false;
            String winners = message.replace("Game over! It's a tie! Winners: ", "").trim();
            JOptionPane.showMessageDialog(this, "انتهت اللعبة! بالتعادل", "Game Over", JOptionPane.INFORMATION_MESSAGE);
            new Thread(() -> SoundPlayer.playSound("success-fanfare-trumpets-6185.wav")).start();
            Winner(winners);
            gameRoom.clear();
            cardLayout.show(mainPanel, "Page 6");
            

        }else if (message.startsWith("Game over! No winner.") && winner ) {
        	winner = false;
            JOptionPane.showMessageDialog(this, "انتهت اللعبة!  لا يوجد فائز", "Game Over", JOptionPane.INFORMATION_MESSAGE);
            new Thread(() -> wrong.playSound("cartoon-trombone-sound-effect-241387.wav")).start();

            Winner("لا يوجد فائز");
            gameRoom.clear();
            cardLayout.show(mainPanel, "Page 6");
           

        }

  }


   

    // Update the list of connected players
    private void updateConnectedPlayers(String message) {
        String[] players = message.replace("Connected players: ", "").split(", ");
        connectedPlayers.clear();
        for (String player : players) {
            connectedPlayers.add(player);
        }

        SwingUtilities.invokeLater(() -> {
            playerListArea.setText(String.join("\n", connectedPlayers));
        });
    }
   
    // Update the waiting list
    private void updateWaitingList(String message) {
        String[] players = message.replace("Waiting list: ", "").split(", ");
        waitingPlayers.clear();
        for (String player : players) {
            waitingPlayers.add(player);
        }

        SwingUtilities.invokeLater(() -> {
            waitingListArea.setText(String.join("\n", waitingPlayers));
        });
    }
  
    // Update the scores
    private void updateScores(String scores) {
         
        String formattedScores = scores.replace(", ", "\n");
        
        if (formattedScores.endsWith("\n")) {
            formattedScores = formattedScores.substring(0, formattedScores.length() - 1);
        }
        
      formattedScores = "\n" + formattedScores;

       final String finalFormattedScores = formattedScores;

        SwingUtilities.invokeLater(() -> {
            scoreArea.setText(finalFormattedScores);
        });
    }

    private void updateCurrentWord(String word) {
        SwingUtilities.invokeLater(() -> currentWord.setText(word));
    }
    
    private void Winner(String winner) {
        SwingUtilities.invokeLater(() -> winnerArea.setText(winner));
    }
    
    //  start the game timer 
   private void startGameTimer() {

        timeRemaining = 50; // Reset the timer 
        synchronized (gameRoom) {
        gameTimer = new Timer(1000, e -> {
            if (timeRemaining > 0) {
                timeRemaining--;
                timerArea.setText("مؤقت:" + timeRemaining);  // Update the timer label
            } else {
                gameTimer.stop();
                out.println("end");
            }
        });
        gameTimer.start();
    }
   }
   
    public static void main(String[] args) {
        SwingUtilities.invokeLater(GameGUI::new);
    }
}



