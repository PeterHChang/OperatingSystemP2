import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.Semaphore;
import java.util.*;

public class Project2 
{

    //set up queues for the line the customer would be in,

    static Queue<Integer> BankCustomerLine = new LinkedList<>(); // BankCustomerLine list of cutomers' unique ID
    static Queue<String> CustomerTransLine = new LinkedList<>(); // CustomerTransLine stores Customer Transaction Type
    static Queue<Integer> TellerStation = new LinkedList<>(); // TellerStation stores available Teller's ID

    static final int Num_Teller = 3; //set up max tellers for initialization
    static final int Num_Customer = 100; //set up max customers for initialization

      /**
     * @param args
     */
    static public void main(String[] args) 
    {
        try {

            for (int i = 0; i < Num_Teller; i++) {
                approachingCustomer[i] = new Semaphore(0);
                askForTransaction[i] = new Semaphore(0);
                tellWaitTransType[i] = new Semaphore(0);
                completedTransType[i] = new Semaphore(0);
                leavingSignal[i] = new Semaphore(0);
            }

            Thread[] tellers = new Thread[Num_Teller];
            Thread[] customers = new Thread[Num_Customer];

            for (int i = 0; i < Num_Teller; i++) {
                tellers[i] = new Teller(i);
                tellers[i].start();
            }

            for (int i = 0; i < Num_Customer; i++) {
                customers[i] = new Customer(i);
                customers[i].start();
            }

            for (int i = 0; i < Num_Teller; i++) {
                tellers[i].join();
            }

            System.out.println("The bank closes for the day.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    
    }

    //end of main

    //initialize Semaphores for Tellers
    static Semaphore tellerIsReady = new Semaphore(1); // if when a teller is ready to serve
    static Semaphore speakToManager = new Semaphore(1); // only 1 teller are allowed to talk to manager 
    static Semaphore tellAtSafe = new Semaphore(2); // only 2 tellers are allowed in the safe
    static Semaphore tellClockOut = new Semaphore(-2); // Semaphore for tellers to leave the bank. when last customer is served. 

    //Semaphore array initialization for Teller

    static Semaphore[] approachingCustomer = new Semaphore[3]; // When a teller is waiting for a customer
    static Semaphore[] tellWaitTransType = new Semaphore[3]; // When Teller is waiting for a customer's transaction type. 
    static Semaphore[] leavingSignal = new Semaphore[3]; // When tellers are ready to leave 

    //Semaphore boolean initialization for tellers
    static boolean tellersisNotWorking = true; // are tellers available?
    static boolean customerInLine = true; // Are there Customers in line?



    // Teller Thread
    public static class Teller extends Thread 
    {
        private int tellerID; //storing a unique ID per Teller

        public Teller(int tellerID) {
            this.tellerID = tellerID;
        }

        private int customerWho; // customer teller is dealing with
        private String customerWhoTransType; // customer type the teller is dealing with

        private static int totalCustomerServed; // Customer Counter total in program.

        Random random = new Random(); // random generatore

        public void tellerSimulation() throws InterruptedException 
        {
            int numCustomerServed = 0; // Number of customer this Teller serves

            do {
                tellerIsReady.acquire(); // Teller is ready signal
                System.out.println("Teller " + this.tellerID + " is ready to serve.");
                
                System.out.println("Teller " + this.tellerID + " is waiting for a customer."); // waiting for customer to show up

                TellerStation.add(this.tellerID); // this teller is available rn

                //wait for teller signal
                if (!tellersisNotWorking && !TellerStation.isEmpty()) {
                    tellersisNotWorking = true;
                    selectwhichTeller.release(); // signal to say teller is ready for choosing
                }

                tellerIsReady.release(); // teller is ready signal
                transType.release(); // cust trans type 

                // oo there is a customer
                approachingCustomer[this.tellerID].acquire();


                //this customer is now being served, so remove from line
                customerWho = BankCustomerLine.remove();

                System.out.println("Teller " + this.tellerID + " is serving Customer " + customerWho + ".");
                totalCustomerServed++; 


                askForTransaction[this.tellerID].release(); // Signal Customer to give their transaction request

                // wait for customer's trans type
                tellWaitTransType[this.tellerID].acquire();
                customerWhoTransType = CustomerTransLine.remove(); // remove this customer's transaction from the queue

                System.out.println("Customer asks for a " + customerWhoTransType + " transaction");

                // Handle transaction request
                System.out.println("Teller " + this.tellerID + " is handling the " + customerWhoTransType + " transaction.");

                
                // if the type is withdrawl, 
                if (customerWhoTransType.equalsIgnoreCase("withdrawal")) 
                {
                    System.out.println("Teller " + this.tellerID + " is going to the manager.");
                    speakToManager.acquire(); //get managers permission
                    System.out.println("Teller " + this.tellerID + " is getting to the manager's permission.");

                    Thread.sleep((random.nextInt(30 - 5) + 5)); // sleep this random amount of time
                    System.out.println("Teller " + this.tellerID + " got the manager's permission.");
                    speakToManager.release(); //signal manager's permission was granted
                }

                System.out.println("Teller " + this.tellerID + " is going to the safe.");
                // go to the safe
                tellAtSafe.acquire();
                System.out.println("Teller " + this.tellerID + " is in the safe.");
                goingtoBank.release(); // next customer enter

                Thread.sleep((random.nextInt(50 - 10) + 10)); // sleep for random

                System.out.println("Teller " + this.tellerID + " is leaving the safe.");
                tellAtSafe.release();
               

                // tell customer the type is finished
                System.out.println("Teller " + this.tellerID + " finishes Customer " + customerWho + "\'s "+ customerWhoTransType + " transaction.");
                numCustomerServed++;
                completedTransType[this.tellerID].release(); // complete transaction signal

                //if that was the final customer, then clock out
                if (totalCustomerServed >= Num_Customer - 2) {
                    tellClockOut.release();
                    break;
                }
            } while (customerInLine);

            // get ready for closing
            leavingSignal[this.tellerID].acquire(numCustomerServed); // wait for each teller to finish
            tellClockOut.acquire(); // final customer
            System.out.println("Teller " + this.tellerID + " is leaving for the day.");
            tellClockOut.release(); // leave this vicinity

        }

        //run the whole mess up there
        @Override
        public void run() {
            try {
                tellerSimulation();
            } catch (Exception e) {
                 // TODO: handle exception
                e.printStackTrace();
                
            }
        }
    }

    //end of Tellers


    //initialize Semaphores for Customers
    static Semaphore[] completedTransType = new Semaphore[3]; // wait for their turn
    static Semaphore[] askForTransaction = new Semaphore[3]; // For customer to wait for a teller


    static Semaphore goingtoBank = new Semaphore(0); // customers now going to bank
    static Semaphore transType = new Semaphore(-2); // For customer to declare transaction
    static Semaphore selectwhichTeller = new Semaphore(0); // For customer to select tellers at the TellerStation


    public static class Customer extends Thread 
    {

        private int CustomerID; // Unique id of the teller
        private int servingTellerID; // Current in-service Teller
        private String[] transTypeRandom = { "withdrawal", "deposit" };
        private String transTypeCustomer; // Type of Customer transaction
        private Random random = new Random(); // Random number generator

        private static int totalTransType; // Customer trans type total counter in program.


        public Customer(int CustomerID) 
        {
            this.CustomerID = CustomerID;
            this.transTypeCustomer = transTypeRandom[random.nextInt(transTypeRandom.length)]; // random trans type
        }


        public void customerSimulation() throws InterruptedException{

            transType.acquire(); // get which type to get
            System.out.println("Customer " + this.CustomerID + " wants to perform a " + this.transTypeCustomer + " transaction.");
            totalTransType++; 

            transType.release(); // release type to teller

            if (totalTransType == Num_Customer) //once its all filled up, release this information
            { 
                goingtoBank.release();
                totalTransType = 0;
            }
            
            goingtoBank.acquire();
            BankCustomerLine.add(this.CustomerID); // add this customer to the line

            System.out.println("Customer " + this.CustomerID + " is going to the bank.");

            Thread.sleep(random.nextInt(5000)); // choose random int for 0 to 5000 ms and block for that long

            System.out.println("Customer " + this.CustomerID + " enters the bank.");
            System.out.println("Customer " + this.CustomerID + " is getting in line.");
            System.out.println("Customer " + this.CustomerID + " is selecting a teller.");

            // Get in BankCustomerLine
            if (TellerStation.isEmpty()) //if there are no stations wait for the available teller
            {
                tellersisNotWorking = false;
                selectwhichTeller.acquire(); 
            }
            servingTellerID = TellerStation.remove(); // go to the station available
            System.out.println("Customer " + this.CustomerID + " goes to Teller " + servingTellerID + ".");

            // introduction
            approachingCustomer[servingTellerID].release(); // signals that teller is serving
            System.out.println("Customer " + this.CustomerID + " introduces itself to Teller " + servingTellerID + ".");

            askForTransaction[servingTellerID].acquire();
            
            // give the transaction type
            System.out.println("Customer " + this.CustomerID + " asks for a " + this.transTypeCustomer + " transaction.");
            CustomerTransLine.add(this.transTypeCustomer); // add the transction type to the queue
            tellWaitTransType[servingTellerID].release(); // give the type to the teller

            completedTransType[servingTellerID].acquire(); // needs to wait until it is fully processed

            //customer leaves this vicinity
            System.out.println("Customer " + this.CustomerID + " thanks Teller " + servingTellerID + " and leaves.");

            //if there are no more customers in line, change the boolean to false to notify the tellers
            if (BankCustomerLine.isEmpty())
                customerInLine = false;
            leavingSignal[servingTellerID].release(); //tellers can now leave
        }
         //run the whole mess up there
        @Override
        public void run() {
            try {
                customerSimulation();
            } catch (Exception e) {
                // TODO: handle exception
                e.printStackTrace();
            }
        }
    }
}

    