CREATE OR REPLACE FUNCTION public.handle_new_user() 
RETURNS TRIGGER AS $$
BEGIN
  INSERT INTO public.profiles (id, full_name, role)
  VALUES (
    new.id, 
    COALESCE(new.raw_user_meta_data->>'full_name', 'Admin/' || split_part(new.email, '@', 1)), 
    'ALUNO'
  );
  RETURN new;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
