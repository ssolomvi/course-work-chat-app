package ru.mai.utils;

public class MathOperations {
    /**
     * @param a is any whole number
     * @param p is a prime number, p > 2
     */
    public static int symbolLegendre(int a, int p) {
        // todo: ask Ilusha if checking p for being prime needed
        if (p < 3) {
            throw new IllegalArgumentException("");
        }

        if (a % p == 0) {
            return 0;
        }

        // Euler criterion:
        // if a^((p - 1) / 2) === 1 (mod p) => a is quadratic residue modulo p
        // if a^((p - 1) / 2) === -1 (mod p) => a is quadratic nonresidue modulo p

        // todo: if resul t != 1 or -1 (-1 === a - 1)
        return fastPowMod(a, (p - 1) / 2, p) == 1 ? 1 : -1;
    }

    /**
     * @param a is any whole number
     * @param P is any positive odd P
     */
    public static int symbolJacobi(int a, int P) {
        if (P <= 0 || (P & 1) == 0) {
            throw new IllegalArgumentException("Param P must be even");
        }

        // Jacobi's symbol properties
        if (P == 1 || a == 1) {
            return 1;
        }

        // calculation Jacobi's symbol
        // step 1
        if (gcd(a, P) != 1 || a == 0) {
            return 0;
        }

        // step 2
        int r = 1;

        // step 3
        if (a < 0) {
            a = -a;
            if (P % 4 == 3) {
                r = -r;
            }
        }

        // step 4
        while (a != 0) {
            int t = 0;
            while ((a & 1) == 0) {
                ++t;
                a /= 2;
            }

            if ((t & 1) == 1) {
                if (P % 8 == 3 || P % 8 == 5) {
                    r = -r;
                }
            }

            // step 5
            if ((a % 4 == 3) && (P % 4 == 3)) {
                r = -r;
            }

            int tmp = a;
            a = P % tmp;
            P = tmp;

            if (a == 1) {
                break;
            }
        }

        // step 6
        return r;
    }

    public static int gcd(int a, int b) {
        // todo: Euclidean algorithm
        if (a == 0 && b == 0) {
            return 0;
        }

        if (a < 0) {
            a = -a;
        }
        if (b < 0) {
            b = -b;
        }

        int reminder;
        while (b != 0) {
            reminder = a % b;
            a = b;
            b = reminder;
        }
//        while (b != 0) {
//            a = a % b;
//            int tmp = b;
//            b = a;
//            a = tmp;
//        }

        return a;
    }

    public static int[] gcdExtended(int a, int b) {
        int rPrev = a, r = b;
        int sPrev = 1, s = 0;
        int tPrev = 0, t = 1;

        while (r != 0) {
            int quotient = rPrev / r;
            int tmp;

            // (rPrev, r) := (r, rPrev − quotient * r)
            tmp = r;
            r = rPrev - quotient * r;
            rPrev = tmp;

            // (sPrev, s) := (s, sPrev − quotient * s)
            tmp = s;
            s = sPrev - quotient * s;
            sPrev = tmp;

            // (tPrev, t) := (t, tPrev − quotient * t)
            tmp = t;
            t = tPrev - quotient * t;
            tPrev = tmp;
        }

        if ((a == 0 && b < 0) || (b == 0 && a < 0) || (rPrev < 0)) {
            rPrev = -rPrev;
            sPrev = -sPrev;
            tPrev = -tPrev;
        }

        // rPrev = gcd, sPrev and tPrev - Bezu coefficients: a * sPrev + b * tPrev = gcd
        return new int[] {rPrev, sPrev, tPrev};
    }

    public static int fastPowMod(int number, int degree, int modulo) {
        if (degree == 0) {
            return 1;
        }

        number %= modulo;
        degree %= modulo;

        int toReturn = 1;
        boolean flag = true;

        if (degree < 0) {
            degree = -degree;
            flag = false;
        }

        while (degree > 0){
            if ((degree & 1) == 1) {
                toReturn *= number;
                toReturn %= modulo;
            }

            number *= number;
            number %= modulo;
            toReturn %= modulo;
            degree >>= 1;
        }

        if (!flag) {
            // todo: возвращать обратное число (ведь мы работаем в конечном поле чисел)
            return 1 / number;
        }

        return toReturn;
    }
}