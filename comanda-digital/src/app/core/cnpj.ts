import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

// RN07 no front (o back confere de novo): mesmo algoritmo do Cnpj.java.
// Aceita o CNPJ numérico e o alfanumérico (12 primeiros caracteres podem ter letras; os 2 últimos são números).
const PESOS_1 = [5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2];
const PESOS_2 = [6, ...PESOS_1];

export function normalizarCnpj(v: string | null | undefined): string {
  return (v ?? '').replace(/[.\-/\s]/g, '').toUpperCase();
}

function digito(base: string, pesos: number[]): number {
  const soma = pesos.reduce((s, p, i) => s + (base.charCodeAt(i) - 48) * p, 0); // '0' = 48, 'A' = 65 (vale 17)
  const resto = soma % 11;
  return resto < 2 ? 0 : 11 - resto;
}

export function cnpjValido(v: string | null | undefined): boolean {
  const c = normalizarCnpj(v);
  if (!/^[0-9A-Z]{12}[0-9]{2}$/.test(c) || new Set(c).size === 1) return false;
  const d1 = digito(c.slice(0, 12), PESOS_1);
  const d2 = digito(c.slice(0, 12) + d1, PESOS_2);
  return Number(c[12]) === d1 && Number(c[13]) === d2;
}

// Coloca a máscara 00.000.000/0000-00 enquanto a pessoa digita (sem cortar o que ainda está incompleto)
export function mascararCnpj(v: string): string {
  const c = normalizarCnpj(v).replace(/[^0-9A-Z]/g, '').slice(0, 14);
  const partes = [c.slice(0, 2), c.slice(2, 5), c.slice(5, 8), c.slice(8, 12), c.slice(12, 14)];
  let r = partes[0];
  if (partes[1]) r += '.' + partes[1];
  if (partes[2]) r += '.' + partes[2];
  if (partes[3]) r += '/' + partes[3];
  if (partes[4]) r += '-' + partes[4];
  return r;
}

export const cnpjValidator: ValidatorFn = (c: AbstractControl): ValidationErrors | null =>
  !c.value || cnpjValido(c.value) ? null : { cnpj: true };
