package main;

import java.util.HashMap;

public class Teste {
    public static void main(String[] args) {
        final Customer c = new Customer("Sally");

        System.out.println("--- Chamando renameCustomer ---");
        renameCustomer(c);
        System.out.println("--- Fim da chamada ---");

        System.out.print("Nome final impresso no main: ");
        System.out.println(c.getName());

        HashMap<String, Customer> teste = new HashMap<>();
        teste.put("aa", c);
    }

    /**
     * Altera o nome do objeto Customer que é passado.
     * * @param cust O objeto Customer cuja propriedade 'name' será alterada.
     */
    public static void renameCustomer(Customer cust) {
        cust.setName("Diane");
    }
}
