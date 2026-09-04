/** The same mistake as before: handing UPI to the split feature. */
public class Bad {
    public static void main(String[] args) {
        System.out.println(SplitTheBill.run(new CardPayment(), 3, 50000));
        System.out.println(SplitTheBill.run(new UpiPayment(), 3, 50000));
    }
}
