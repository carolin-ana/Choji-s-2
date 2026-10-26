package com.comanda.validation;

// RN07: validação do CNPJ pelo algoritmo dos dígitos verificadores (módulo 11).
// Aceita o CNPJ numérico e também o alfanumérico da Receita Federal (em uso desde julho de 2026):
// as 12 primeiras posições podem ter letras e os 2 dígitos verificadores são sempre números.
// Cada caractere vale (código ASCII − 48): '0'..'9' valem 0..9 e 'A'..'Z' valem 17..42.
public final class Cnpj {
    private static final int[] PESOS_1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final int[] PESOS_2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

    private Cnpj() {}

    // Tira pontos, barra, traço e espaços e passa para maiúsculas: "47.218.336/0001-99" -> "47218336000199"
    public static String normalizar(String cnpj) {
        return cnpj == null ? null : cnpj.replaceAll("[.\\-/\\s]", "").toUpperCase();
    }

    public static boolean valido(String cnpj) {
        String c = normalizar(cnpj);
        if (c == null || !c.matches("[0-9A-Z]{12}[0-9]{2}")) return false;
        if (c.chars().distinct().count() == 1) return false; // 00000000000000, 11111111111111... passam na conta, mas não existem
        int dv1 = digito(c.substring(0, 12), PESOS_1);
        int dv2 = digito(c.substring(0, 12) + dv1, PESOS_2);
        return c.charAt(12) - '0' == dv1 && c.charAt(13) - '0' == dv2;
    }

    // "47218336000199" -> "47.218.336/0001-99"
    public static String formatar(String cnpj) {
        String c = normalizar(cnpj);
        if (c == null || c.length() != 14) return cnpj;
        return c.substring(0, 2) + "." + c.substring(2, 5) + "." + c.substring(5, 8) + "/" + c.substring(8, 12) + "-" + c.substring(12);
    }

    private static int digito(String base, int[] pesos) {
        int soma = 0;
        for (int i = 0; i < pesos.length; i++) soma += (base.charAt(i) - '0') * pesos[i];
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }
}
