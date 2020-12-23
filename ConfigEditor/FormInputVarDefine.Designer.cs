
namespace ConfigEditor
{
    partial class FormInputVarDefine
    {
        /// <summary>
        /// Required designer variable.
        /// </summary>
        private System.ComponentModel.IContainer components = null;

        /// <summary>
        /// Clean up any resources being used.
        /// </summary>
        /// <param name="disposing">true if managed resources should be disposed; otherwise, false.</param>
        protected override void Dispose(bool disposing)
        {
            if (disposing && (components != null))
            {
                components.Dispose();
            }
            base.Dispose(disposing);
        }

        #region Windows Form Designer generated code

        /// <summary>
        /// Required method for Designer support - do not modify
        /// the contents of this method with the code editor.
        /// </summary>
        private void InitializeComponent()
        {
            this.textBoxVarName = new System.Windows.Forms.TextBox();
            this.checkBoxIsList = new System.Windows.Forms.CheckBox();
            this.textBoxListRefBeanName = new System.Windows.Forms.TextBox();
            this.label1 = new System.Windows.Forms.Label();
            this.label2 = new System.Windows.Forms.Label();
            this.buttonCancel = new System.Windows.Forms.Button();
            this.buttonOk = new System.Windows.Forms.Button();
            this.textBox1 = new System.Windows.Forms.TextBox();
            this.SuspendLayout();
            // 
            // textBoxVarName
            // 
            this.textBoxVarName.Location = new System.Drawing.Point(111, 12);
            this.textBoxVarName.Name = "textBoxVarName";
            this.textBoxVarName.Size = new System.Drawing.Size(521, 35);
            this.textBoxVarName.TabIndex = 0;
            // 
            // checkBoxIsList
            // 
            this.checkBoxIsList.AutoSize = true;
            this.checkBoxIsList.Location = new System.Drawing.Point(111, 78);
            this.checkBoxIsList.Name = "checkBoxIsList";
            this.checkBoxIsList.Size = new System.Drawing.Size(162, 28);
            this.checkBoxIsList.TabIndex = 1;
            this.checkBoxIsList.Text = "是否为List";
            this.checkBoxIsList.UseVisualStyleBackColor = true;
            // 
            // textBoxListRefBeanName
            // 
            this.textBoxListRefBeanName.Location = new System.Drawing.Point(111, 112);
            this.textBoxListRefBeanName.Name = "textBoxListRefBeanName";
            this.textBoxListRefBeanName.Size = new System.Drawing.Size(521, 35);
            this.textBoxListRefBeanName.TabIndex = 2;
            // 
            // label1
            // 
            this.label1.AutoSize = true;
            this.label1.Location = new System.Drawing.Point(23, 23);
            this.label1.Name = "label1";
            this.label1.Size = new System.Drawing.Size(82, 24);
            this.label1.TabIndex = 3;
            this.label1.Text = "列名：";
            // 
            // label2
            // 
            this.label2.AutoSize = true;
            this.label2.Location = new System.Drawing.Point(23, 123);
            this.label2.Name = "label2";
            this.label2.Size = new System.Drawing.Size(82, 24);
            this.label2.TabIndex = 4;
            this.label2.Text = "Bean：";
            // 
            // buttonCancel
            // 
            this.buttonCancel.DialogResult = System.Windows.Forms.DialogResult.Cancel;
            this.buttonCancel.Location = new System.Drawing.Point(429, 313);
            this.buttonCancel.Name = "buttonCancel";
            this.buttonCancel.Size = new System.Drawing.Size(173, 60);
            this.buttonCancel.TabIndex = 5;
            this.buttonCancel.Text = "取消";
            this.buttonCancel.UseVisualStyleBackColor = true;
            // 
            // buttonOk
            // 
            this.buttonOk.DialogResult = System.Windows.Forms.DialogResult.OK;
            this.buttonOk.Location = new System.Drawing.Point(223, 313);
            this.buttonOk.Name = "buttonOk";
            this.buttonOk.Size = new System.Drawing.Size(186, 60);
            this.buttonOk.TabIndex = 6;
            this.buttonOk.Text = "确定";
            this.buttonOk.UseVisualStyleBackColor = true;
            // 
            // textBox1
            // 
            this.textBox1.BackColor = System.Drawing.SystemColors.Menu;
            this.textBox1.BorderStyle = System.Windows.Forms.BorderStyle.None;
            this.textBox1.Location = new System.Drawing.Point(27, 167);
            this.textBox1.Multiline = true;
            this.textBox1.Name = "textBox1";
            this.textBox1.ReadOnly = true;
            this.textBox1.Size = new System.Drawing.Size(605, 118);
            this.textBox1.TabIndex = 8;
            this.textBox1.Text = "不指定Bean时，会自动创建该列专用的Bean。指定的Bean必须使用完整的名字（如ConfigFile.AnotherBean）。可以引用定义在其他文件中Bea" +
    "n。指定Bean时，必须已经定义。";
            // 
            // FormInputVarDefine
            // 
            this.AcceptButton = this.buttonOk;
            this.AutoScaleDimensions = new System.Drawing.SizeF(12F, 24F);
            this.AutoScaleMode = System.Windows.Forms.AutoScaleMode.Font;
            this.CancelButton = this.buttonCancel;
            this.ClientSize = new System.Drawing.Size(670, 399);
            this.Controls.Add(this.textBox1);
            this.Controls.Add(this.buttonOk);
            this.Controls.Add(this.buttonCancel);
            this.Controls.Add(this.label2);
            this.Controls.Add(this.label1);
            this.Controls.Add(this.textBoxListRefBeanName);
            this.Controls.Add(this.checkBoxIsList);
            this.Controls.Add(this.textBoxVarName);
            this.Name = "FormInputVarDefine";
            this.Text = "输入列信息";
            this.Load += new System.EventHandler(this.FormInputVarDefine_Load);
            this.ResumeLayout(false);
            this.PerformLayout();

        }

        #endregion

        private System.Windows.Forms.TextBox textBoxVarName;
        private System.Windows.Forms.CheckBox checkBoxIsList;
        private System.Windows.Forms.TextBox textBoxListRefBeanName;
        private System.Windows.Forms.Label label1;
        private System.Windows.Forms.Label label2;
        private System.Windows.Forms.Button buttonCancel;
        private System.Windows.Forms.Button buttonOk;
        private System.Windows.Forms.TextBox textBox1;
    }
}