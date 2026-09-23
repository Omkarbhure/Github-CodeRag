import { createClient } from '@supabase/supabase-js';

const supabaseUrl = process.env.NEXT_PUBLIC_SUPABASE_URL || 'https://mgbekbdmxltwfbzwvzgq.supabase.co';
const supabaseAnonKey =
  process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY ||
  process.env.NEXT_PUBLIC_SUPABASE_PUBLISHABLE_KEY ||
  'sb_publishable_6HCyqcfihvTeNGU_W37M3w_iwybeVVp';

export const supabase = createClient(supabaseUrl, supabaseAnonKey);
